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

import java.nio.charset.Charset;
import java.util.EnumSet;

import com.pushtechnology.diffusion.client.Diffusion;
import com.pushtechnology.diffusion.client.details.SessionDetails;
import com.pushtechnology.diffusion.client.details.SessionDetails.DetailType;
import com.pushtechnology.diffusion.client.features.ServerHandler;
import com.pushtechnology.diffusion.client.features.control.clients.AuthenticationControl;
import com.pushtechnology.diffusion.client.features.control.clients.AuthenticationControl.ControlAuthenticationHandler;
import com.pushtechnology.diffusion.client.session.Session;
import com.pushtechnology.diffusion.client.types.Credentials;

/**
 * This demonstrates the use of a control client to authenticate client
 * connections.
 * <P>
 * This uses the 'AuthenticationControl' feature.
 *
 * @author Push Technology Limited
 * @since 5.0
 */
public class ControlClientIdentityChecks {

    private final Session session;

    /**
     * Constructor.
     */
    public ControlClientIdentityChecks() {

        session =
            Diffusion.sessions().principal("control").password("password")
                .open("ws://diffusion.example.com:80");

        final AuthenticationControl authenticationControl =
            session.feature(AuthenticationControl.class);

        // To register the authentication handler, this client session must
        // have the 'authenticate' and 'register_handler' permissions.
        authenticationControl.setAuthenticationHandler(
            "example-handler",
            EnumSet.allOf(DetailType.class),
            new Handler());
    }

    /**
     * Authentication handler.
     */
    private static class Handler extends ServerHandler.Default
        implements ControlAuthenticationHandler {
        @Override
        public void authenticate(
            final String principal,
            final Credentials credentials,
            final SessionDetails sessionDetails,
            final Callback callback) {

            final byte[] passwordBytes =
                "password".getBytes(Charset.forName("UTF-8"));

            if ("admin".equals(principal) &&
                credentials.getType() == Credentials.Type.PLAIN_PASSWORD &&
                credentials.toBytes().equals(passwordBytes)) {
                callback.allow();
            }
            else {
                callback.deny();
            }
        }
    }

    /**
     * Close the session.
     */
    public void close() {
        session.close();
    }
}
