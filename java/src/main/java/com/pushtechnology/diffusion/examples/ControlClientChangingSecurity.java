/*******************************************************************************
 * Copyright (C) 2014, 2015 Push Technology Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.pushtechnology.diffusion.examples;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pushtechnology.diffusion.client.Diffusion;
import com.pushtechnology.diffusion.client.callbacks.ErrorReason;
import com.pushtechnology.diffusion.client.features.control.clients.SecurityControl;
import com.pushtechnology.diffusion.client.features.control.clients.SecurityControl.ConfigurationCallback;
import com.pushtechnology.diffusion.client.features.control.clients.SecurityControl.Role;
import com.pushtechnology.diffusion.client.features.control.clients.SecurityControl.ScriptBuilder;
import com.pushtechnology.diffusion.client.features.control.clients.SecurityControl.SecurityConfiguration;
import com.pushtechnology.diffusion.client.features.control.clients.SecurityStoreFeature.UpdateStoreCallback;
import com.pushtechnology.diffusion.client.session.Session;
import com.pushtechnology.diffusion.client.types.GlobalPermission;
import com.pushtechnology.diffusion.client.types.TopicPermission;

/**
 * An example of using a control client to alter the security configuration.
 * <P>
 * This uses the {@link SecurityControl} feature only.
 *
 * @author Push Technology Limited
 * @since 5.3
 */
public class ControlClientChangingSecurity {

    private static final Logger LOG =
        LoggerFactory.getLogger(
            ControlClientChangingSecurity.class);

    private final SecurityControl securityControl;

    /**
     * Constructor.
     */
    public ControlClientChangingSecurity() {

        final Session session = Diffusion.sessions()
            // Authenticate with a user that has the VIEW_SECURITY and
            // MODIFY_SECURITY permissions.
            .principal("admin").password("password")
            // Use a secure channel because we're transferring sensitive
            // information.
            .open("wss://diffusion.example.com:80");

        securityControl = session.feature(SecurityControl.class);
    }

    /**
     * This will update the security store to ensure that all roles start with a
     * capital letter (note that this does not address changing the use of the
     * roles in the system authentication store).
     *
     * @param callback result callback
     */
    public void capitalizeRoles(UpdateStoreCallback callback) {
        securityControl.getSecurity(new CapitalizeRoles(callback));
    }

    private final class CapitalizeRoles implements ConfigurationCallback {

        private final UpdateStoreCallback callback;

        CapitalizeRoles(UpdateStoreCallback callback) {
            this.callback = callback;
        }

        @Override
        public void onReply(SecurityConfiguration configuration) {

            ScriptBuilder builder =
                securityControl.scriptBuilder();

            builder = builder.setRolesForAnonymousSessions(
                capitalize(configuration.getRolesForAnonymousSessions()));

            builder = builder.setRolesForNamedSessions(
                capitalize(configuration.getRolesForNamedSessions()));

            for (Role role : configuration.getRoles()) {

                final String oldName = role.getName();
                final String newName = capitalize(oldName);

                // Only if new name is different
                if (!oldName.equals(newName)) {

                    // Global Permissions
                    final Set<GlobalPermission> globalPermissions =
                        role.getGlobalPermissions();
                    if (!globalPermissions.isEmpty()) {
                        // Remove global permissions for old role
                        builder =
                            builder.setGlobalPermissions(
                                oldName,
                                Collections.<GlobalPermission>emptySet());
                        // Set global permissions for new role
                        builder =
                            builder.setGlobalPermissions(
                                newName,
                                role.getGlobalPermissions());
                    }

                    final Set<TopicPermission> defaultTopicPermissions =
                        role.getDefaultTopicPermissions();
                    if (!defaultTopicPermissions.isEmpty()) {
                        // Remove default topic permissions for old role
                        builder =
                            builder.setDefaultTopicPermissions(
                                oldName,
                                Collections.<TopicPermission>emptySet());
                        // Set default topic permissions for new role
                        builder =
                            builder.setDefaultTopicPermissions(
                                newName,
                                role.getDefaultTopicPermissions());
                    }

                    final Map<String, Set<TopicPermission>> topicPermissions =
                        role.getTopicPermissions();

                    if (!topicPermissions.isEmpty()) {
                        for (Map.Entry<String, Set<TopicPermission>> entry : topicPermissions
                            .entrySet()) {
                            final String topicPath = entry.getKey();
                            // Remove old topic permissions
                            builder =
                                builder.removeTopicPermissions(
                                    oldName,
                                    topicPath);
                            // Set new topic permissions
                            builder =
                                builder.setTopicPermissions(
                                    newName,
                                    topicPath,
                                    entry.getValue());
                        }
                    }

                }

                final Set<String> oldIncludedRoles = role.getIncludedRoles();
                if (!oldIncludedRoles.isEmpty()) {

                    if (!oldName.equals(newName)) {
                        // Remove old included roles
                        builder =
                            builder.setRoleIncludes(
                                oldName,
                                Collections.<String>emptySet());
                    }

                    // This is done even if role name did not change as it is
                    // possible that roles included may have
                    final Set<String> newIncludedRoles =
                        capitalize(oldIncludedRoles);
                    builder =
                        builder.setRoleIncludes(
                            newName,
                            newIncludedRoles);

                }


            }

            final String script = builder.script();

            LOG.info(
                "Sending the following script to the server:\n{}",
                script);

            securityControl.updateStore(
                script,
                callback);
        }

        private Set<String> capitalize(Set<String> roles) {
            final Set<String> newSet = new TreeSet<>();
            for (String role : roles) {
                newSet.add(capitalize(role));
            }
            return newSet;
        }

        private String capitalize(String role) {
            return Character.toUpperCase(role.charAt(0)) + role.substring(1);
        }

        @Override
        public void onError(ErrorReason errorReason) {
            // This might fail if the session lacks the required permissions.
            callback.onError(errorReason);
        }
    }

    /**
     * Close the session.
     */
    public void close() {
        securityControl.getSession().close();
    }
}
