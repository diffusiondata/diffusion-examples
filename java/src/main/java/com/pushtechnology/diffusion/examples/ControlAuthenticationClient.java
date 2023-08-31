/*******************************************************************************
 * Copyright (C) 2014, 2023 DiffusionData Ltd.
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

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.pushtechnology.diffusion.client.Diffusion;
import com.pushtechnology.diffusion.client.callbacks.Stream;
import com.pushtechnology.diffusion.client.features.control.clients.AuthenticationControl;
import com.pushtechnology.diffusion.client.features.control.clients.AuthenticationControl.ControlAuthenticator;
import com.pushtechnology.diffusion.client.session.Session;
import com.pushtechnology.diffusion.client.types.Credentials;

/**
 * This is a control client which registers an authentication handler with a
 * Diffusion server.
 *
 * @author DiffusionData Limited
 * @since 5.0
 */
public final class ControlAuthenticationClient {

    private ControlAuthenticationClient() {
    }

    /**
     * Main entry point for the control client.
     */
    // CHECKSTYLE.OFF: UncommentedMain
    public static void main(final String[] args) throws Exception {

        // The control client connects to the server using the principal 'admin'
        // which is authenticated by the system authentication handler (see
        // etc/SystemAuthentication.store).
        // The principal must have REGISTER_HANDLER and AUTHENTICATE permissions.
        final Session session =
            Diffusion.sessions()
                .principal("admin")
                .password("password")
                .open("ws://diffusion.example.com:80");

        session.feature(AuthenticationControl.class).setAuthenticationHandler(
            "after-system-handler",
            new ExampleControlAuthenticationHandler()).get(10, TimeUnit.SECONDS);

        while (true) {
            Thread.sleep(60000);
        }
    }
    // CHECKSTYLE.ON: UncommentedMain

    /**
     * An example of a control authentication handler.
     * <p>
     * This shows a simple example using a table of permitted principals with
     * their passwords. It also demonstrates how the handler can change the
     * properties of the client being authenticated.
     */
    private static class ExampleControlAuthenticationHandler
        extends Stream.Default
        implements ControlAuthenticator {

        private static final Map<String, byte[]> PASSWORDS = new HashMap<>();
        static {
            PASSWORDS.put("manager", "password".getBytes(Charset.forName("UTF-8")));
            PASSWORDS.put("guest", "asecret".getBytes(Charset.forName("UTF-8")));
            PASSWORDS.put("brian", "boru".getBytes(Charset.forName("UTF-8")));
            PASSWORDS.put("another", "apassword".getBytes(Charset.forName("UTF-8")));
        }

        @Override
        public void authenticate(
            String principal,
            Credentials credentials,
            Map<String, String> sessionProperties,
            Map<String, String> proposedProperties,
            Callback callback) {

            final byte[] passwordBytes = PASSWORDS.get(principal);

            if (passwordBytes != null &&
                credentials.getType() == Credentials.Type.PLAIN_PASSWORD &&
                Arrays.equals(credentials.toBytes(), passwordBytes)) {
                if ("manager".equals(principal)) {
                    // manager allows all proposed properties
                    callback.allow(proposedProperties);
                }
                else if ("brian".equals(principal)) {
                    // brian is allowed all proposed properties and also gets
                    // the 'super' role added
                    final Map<String, String> result =
                        new HashMap<>(proposedProperties);
                    final Set<String> roles =
                        Diffusion.stringToRoles(
                            sessionProperties.get(Session.ROLES));
                    roles.add("super");
                    result.put(Session.ROLES, Diffusion.rolesToString(roles));
                    callback.allow(result);
                }
                else {
                    // all others authenticated but ignoring proposed properties
                    callback.allow();
                }
            }
            else {
                // Any principal not in the table is denied.
                callback.deny();
            }
        }
    }
}
