/*******************************************************************************
 * Copyright (C) 2023 - 2024 DiffusionData Ltd.
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
package com.pushtechnology.client.sdk.example.sessionmanagement.authenticationcontrol;

import com.pushtechnology.diffusion.client.Diffusion;
import com.pushtechnology.diffusion.client.callbacks.ErrorReason;
import com.pushtechnology.diffusion.client.features.control.clients.AuthenticationControl;
import com.pushtechnology.diffusion.client.features.control.clients.AuthenticationControl.ControlAuthenticator;
import com.pushtechnology.diffusion.client.session.AuthenticationException;
import com.pushtechnology.diffusion.client.session.Session;
import com.pushtechnology.diffusion.client.session.SessionFactory;
import com.pushtechnology.diffusion.client.types.Credentials;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class AuthenticationControlExample {

    private static final Logger LOG =
        LoggerFactory.getLogger(AuthenticationControlExample.class);

    public static void main(String[] args) {

        final SessionFactory sessions = Diffusion.sessions();

        final Session controlSession = sessions
            .principal("control")
            .password("password")
            .open("ws://localhost:8080");

        controlSession.feature(AuthenticationControl.class)
            .setAuthenticationHandler("before-system-handler", new MyAuthenticator()).join();

        try {
            sessions.principal("").open("ws://localhost:8080");
        }
        catch (AuthenticationException e) {
            System.out.println("This should fail");
            LOG.info(e.getMessage());
        }

        try {
            sessions.principal("client").password("password")
                .open("ws://localhost:8080");
        }
        catch (AuthenticationException e) {
            System.out.println("This should also fail");
            LOG.info(e.getMessage());
        }

        Session session = sessions
            .principal("diffusion_client")
            .password("password")
            .open("ws://localhost:8080");

        System.out.println("Connected as: " + session.getPrincipal());

        session.close();
        controlSession.close();

        LOG.info(session.getState().toString());
    }

    private static class MyAuthenticator implements ControlAuthenticator {

        @Override
        public void authenticate(String principal, Credentials credentials,
            Map<String, String> sessionProperties,
            Map<String, String> proposedProperties,
            Callback callback) {

            if ("".equals(principal)) {
                System.out.println("Anonymous connection attempt detected. Session establishment Rejected.");
                callback.deny();
                return;
            }

            if (!principal.startsWith("diffusion_")) {
                System.out.println("Principal does not begin with diffusion_ prefix. Session establishment Rejected.");
                callback.deny();
                return;
            }

            System.out.println("Principal begins with diffusion_ prefix. Session establishment Accepted.");
            callback.allow();
        }

        @Override
        public void onClose() {}

        @Override
        public void onError(ErrorReason errorReason) {}
    }
}
