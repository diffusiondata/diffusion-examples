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

import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pushtechnology.diffusion.client.Diffusion;
import com.pushtechnology.diffusion.client.callbacks.ErrorReason;
import com.pushtechnology.diffusion.client.features.control.clients.SystemAuthenticationControl;
import com.pushtechnology.diffusion.client.features.control.clients.SystemAuthenticationControl.ConfigurationCallback;
import com.pushtechnology.diffusion.client.features.control.clients.SystemAuthenticationControl.ScriptBuilder;
import com.pushtechnology.diffusion.client.features.control.clients.SystemAuthenticationControl.SystemAuthenticationConfiguration;
import com.pushtechnology.diffusion.client.features.control.clients.SystemAuthenticationControl.SystemPrincipal;
import com.pushtechnology.diffusion.client.features.control.clients.SecurityStoreFeature.UpdateStoreCallback;
import com.pushtechnology.diffusion.client.session.Session;

/**
 * An example of using a control client to alter the system authentication
 * configuration.
 * <P>
 * This uses the {@link SystemAuthenticationControl} feature only.
 *
 * @author Push Technology Limited
 * @since 5.2
 */
public class ControlClientChangingSystemAuthentication {

    private static final Logger LOG =
        LoggerFactory.getLogger(
            ControlClientChangingSystemAuthentication.class);

    private final SystemAuthenticationControl systemAuthenticationControl;

    /**
     * Constructor.
     */
    public ControlClientChangingSystemAuthentication() {

        final Session session = Diffusion.sessions()
            // Authenticate with a user that has the VIEW_SECURITY and
            // MODIFY_SECURITY permissions.
            .principal("admin").password("password")
            // Use a secure channel because we're transferring sensitive
            // information.
            .open("wss://diffusion.example.com:80");

        systemAuthenticationControl =
            session.feature(SystemAuthenticationControl.class);
    }

    /**
     * For all system users, update the assigned roles to replace the
     * "SUPERUSER" role and with "ADMINISTRATOR".
     *
     * @param callback result callback
     */
    public void changeSuperUsersToAdministrators(UpdateStoreCallback callback) {

        systemAuthenticationControl.getSystemAuthentication(
            new ChangeSuperUsersToAdministrators(callback));
    }

    private final class ChangeSuperUsersToAdministrators
        implements ConfigurationCallback {

        private final UpdateStoreCallback callback;

        ChangeSuperUsersToAdministrators(UpdateStoreCallback callback) {
            this.callback = callback;
        }

        @Override
        public void onReply(SystemAuthenticationConfiguration configuration) {

            ScriptBuilder builder =
                systemAuthenticationControl.scriptBuilder();

            // For all system users ...
            for (SystemPrincipal principal : configuration.getPrincipals()) {

                final Set<String> assignedRoles = principal.getAssignedRoles();

                // ... that have the SUPERUSER assigned role ...
                if (assignedRoles.contains("SUPERUSER")) {
                    final Set<String> newRoles = new HashSet<>(assignedRoles);
                    newRoles.remove("SUPERUSER");
                    newRoles.add("ADMINISTRATOR");

                    // ... add a command to the script that updates the user's
                    // assigned roles, replacing SUPERUSER with "ADMINISTRATOR".
                    builder =
                        builder.assignRoles(principal.getName(), newRoles);
                }
            }

            final String script = builder.script();

            LOG.info(
                "Sending the following script to the server:\n{}",
                script);

            systemAuthenticationControl.updateStore(
                script,
                callback);
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
        systemAuthenticationControl.getSession().close();
    }
}
