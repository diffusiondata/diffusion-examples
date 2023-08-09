/*******************************************************************************
 * Copyright (C) 2014, 2020 Push Technology Ltd.
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

import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toCollection;

import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pushtechnology.diffusion.client.Diffusion;
import com.pushtechnology.diffusion.client.features.control.clients.SecurityControl;
import com.pushtechnology.diffusion.client.features.control.clients.SecurityControl.Role;
import com.pushtechnology.diffusion.client.features.control.clients.SecurityControl.ScriptBuilder;
import com.pushtechnology.diffusion.client.features.control.clients.SecurityControl.SecurityConfiguration;
import com.pushtechnology.diffusion.client.session.Session;
import com.pushtechnology.diffusion.client.types.PathPermission;

/**
 * An example of using a control client to alter the security configuration.
 * <P>
 * This uses the {@link SecurityControl} feature only.
 *
 * @author DiffusionData Limited
 * @since 5.3
 */
public class ControlClientChangingSecurity {

    private static final Logger LOG =
        LoggerFactory.getLogger(
            ControlClientChangingSecurity.class);

    private final SecurityControl securityControl;
    private final ScriptBuilder emptyScript;

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
        emptyScript = securityControl.scriptBuilder();
    }

    /**
     * This will update the security store to ensure that all roles start with a
     * capital letter (note that this does not address changing the use of the
     * roles in the system authentication store).
     *
     * @return a CompletableFuture that completes when the operation succeeds or
     *         fails.
     *
     *         <p>
     *         If the operation was successful, the CompletableFuture will
     *         complete successfully.
     *
     *         <p>
     *         Otherwise, the CompletableFuture will complete exceptionally with
     *         an {@link ExecutionException}. See
     *         {@link SecurityControl#getSecurity()} and
     *         {@link SecurityControl#updateStore(String)} for common failure
     *         reasons.
     */
    public CompletableFuture<Void> capitalizeRoles() {
        return securityControl.getSecurity().thenCompose(this::capitalizeRoles);
    }

    private CompletableFuture<Void> capitalizeRoles(
        SecurityConfiguration configuration) {

        final String script = emptyScript

            .setRolesForAnonymousSessions(
                capitalizeSet(configuration.getRolesForAnonymousSessions()))

            .setRolesForNamedSessions(
                capitalizeSet(configuration.getRolesForNamedSessions()))

            .append(configuration
                // For each role ...
                .getRoles().stream()
                // ... build a script that capitalises that role ...
                .map(this::capitalizeRole)
                /// .. and combine the per-role scripts into one.
                .reduce(emptyScript, (sb1, sb2) -> sb1.append(sb2)))

            .script();

        LOG.info("Sending the following script to the server:\n{}", script);

        return securityControl.updateStore(script)
            // Convert CompletableFuture<?> to CompletableFuture<Void>.
            .thenAccept(ignored -> { });
    }

    private ScriptBuilder capitalizeRole(Role role) {
        final String oldName = role.getName();
        final String newName = capitalizeString(oldName);

        ScriptBuilder builder = emptyScript;

        // Only if new name is different
        if (!oldName.equals(newName)) {
            if (!role.getGlobalPermissions().isEmpty()) {
                builder = builder
                    // Remove global permissions for old role
                    .setGlobalPermissions(oldName, emptySet())
                    // Set global permissions for new role
                    .setGlobalPermissions(
                        newName, role.getGlobalPermissions());
            }

            if (!role.getDefaultPathPermissions().isEmpty()) {
                builder = builder
                    // Remove default path permissions for old role
                    .setDefaultPathPermissions(oldName, emptySet())
                    // Set default path permissions for new role
                    .setDefaultPathPermissions(
                        newName, role.getDefaultPathPermissions());
            }

            builder = builder.append(
                role.getPathPermissions().entrySet().stream().map(
                    entry -> {
                        final String path = entry.getKey();
                        final Set<PathPermission> permissions = entry.getValue();

                        return emptyScript
                            // Remove path permissions for old role
                            .removePathPermissions(oldName, path)
                            // Set path permissions for new role
                            .setPathPermissions(newName, path, permissions);
                    })
                    .reduce(emptyScript, (sb1, sb2) -> sb1.append(sb2)));
        }

        final Set<String> oldIncludedRoles = role.getIncludedRoles();

        if (oldIncludedRoles.isEmpty()) {
            return builder;
        }

        return builder
            // Remove old included roles.
            .setRoleIncludes(oldName, emptySet())

            // Set new roles even if role name did not change as the included
            // roles may be changed.
            .setRoleIncludes(newName, capitalizeSet(oldIncludedRoles));
    }

    private static Set<String> capitalizeSet(Set<String> roles) {
        return roles.stream()
            .map(ControlClientChangingSecurity::capitalizeString)
            .collect(toCollection(TreeSet::new));
    }

    private static String capitalizeString(String role) {
        return Character.toUpperCase(role.charAt(0)) + role.substring(1);
    }

    /**
     * Close the session.
     */
    public void close() {
        securityControl.getSession().close();
    }
}
