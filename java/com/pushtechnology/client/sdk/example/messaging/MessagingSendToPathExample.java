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
package com.pushtechnology.client.sdk.example.messaging;

import static java.util.concurrent.TimeUnit.SECONDS;

import com.pushtechnology.diffusion.client.Diffusion;
import com.pushtechnology.diffusion.client.callbacks.ErrorReason;
import com.pushtechnology.diffusion.client.callbacks.Registration;
import com.pushtechnology.diffusion.client.features.Messaging;
import com.pushtechnology.diffusion.client.session.Session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MessagingSendToPathExample {

    private static final Logger LOG =
        LoggerFactory.getLogger(MessagingSendToPathExample.class);

    public static void main(String[] args)
        throws Exception {

        final String serverUrl = "ws://localhost:8080";

        try (Session session = Diffusion.sessions()
            .principal("admin")
            .password("password")
            .open(serverUrl)) {

            final String path = "my/message/path";

            final Registration registration = session.feature(Messaging.class)
                .addRequestHandler(path, String.class, String.class, new MyRequestHandler())
                .join();

            final Session session2 = Diffusion.sessions()
                .principal("admin").password("password").open(serverUrl);

            final String result = session2.feature(Messaging.class)
                .sendRequest(path, "Hello", String.class, String.class)
                .join();

            LOG.info("Response: '{}'.", result);

            SECONDS.sleep(2);

            registration.close()
                .join();

            session2.close();
        }
    }

    public static class MyRequestHandler implements Messaging.RequestHandler<String, String> {
        private static final Logger LOG = LoggerFactory.getLogger(MyRequestHandler.class);

        @Override
        public void onRequest(
            String request,
            RequestContext requestContext,
            Responder<String> responder) {

            LOG.info("Received message: '{}'.", request);

            responder.respond("Goodbye.");
        }

        @Override
        public void onClose() {
            LOG.info("On close.");
        }

        @Override
        public void onError(ErrorReason errorReason) {
            LOG.info("On error: {}.", errorReason);
        }
    }
}
