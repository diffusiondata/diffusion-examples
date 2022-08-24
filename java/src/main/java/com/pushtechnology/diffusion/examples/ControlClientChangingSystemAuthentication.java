/*******************************************************************************
 * Copyright (C) 2014, 2018 Push Technology Ltd.
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pushtechnology.diffusion.client.Diffusion;
import com.pushtechnology.diffusion.client.features.control.clients.SystemAuthenticationControl;
import com.pushtechnology.diffusion.client.features.control.clients.SystemAuthenticationControl.ScriptBuilder;
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
    private final ScriptBuilder emptyScript;

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
        emptyScript = systemAuthenticationControl.scriptBuilder();
    }

    /**
     * For all system users, update the assigned roles to replace the
     * "SUPERUSER" role and with "ADMINISTRATOR".
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
     *         {@link SystemAuthenticationControl#getSystemAuthentication()} and
     *         {@link SystemAuthenticationControl#updateStore(String)} for
     *         common failure reasons.
     */
    public CompletableFuture<Void> changeSuperUsersToAdministrators() {

        return systemAuthenticationControl
            .getSystemAuthentication()
            .thenCompose(configuration -> {

                final String script = configuration

                    // For each principal ...
                    .getPrincipals().stream()

                    // ... that has the SUPERUSER assigned role ...
                    .filter(p -> p.getAssignedRoles().contains("SUPERUSER"))

                    // ... create a script that updates the assigned roles to
                    // replace SUPERUSER with ADMINISTRATOR ...
                    .map(p -> {
                        final Set<String> newRoles =
                            new HashSet<>(p.getAssignedRoles());
                        newRoles.remove("SUPERUSER");
                        newRoles.add("ADMINISTRATOR");
                        return emptyScript.assignRoles(p.getName(), newRoles);
                    })

                    // ... create a single combined script.
                    .reduce(emptyScript, (sb1, sb2) -> sb1.append(sb2))
                    .script();

                LOG.info("Sending the following script to the server:\n{}",
                    script);

                return systemAuthenticationControl.updateStore(script)
                    // Convert CompletableFuture<?> to
                    // CompletableFuture<Void>.
                    .thenAccept(ignored -> { });
            });
    }

    /**
     * Close the session.
     */
    public void close() {
        systemAuthenticationControl.getSession().close();
    }
}
