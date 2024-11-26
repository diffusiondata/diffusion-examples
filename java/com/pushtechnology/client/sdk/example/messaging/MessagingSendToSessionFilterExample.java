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
import com.pushtechnology.diffusion.client.features.Messaging;
import com.pushtechnology.diffusion.client.session.Session;
import com.pushtechnology.diffusion.client.session.SessionId;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MessagingSendToSessionFilterExample {

    private static final Logger LOG =
        LoggerFactory.getLogger(MessagingSendToSessionFilterExample.class);

    public static void main(String[] args)
        throws Exception {

        final String serverUrl = "ws://localhost:8080";

        try (Session session1 = Diffusion.sessions()
            .principal("admin").password("password").open(serverUrl)) {

            final String path = "my/message/path";

            session1.feature(Messaging.class).setRequestStream(path, String.class, String.class,
                new MyRequestStream1());

            final Session session2 = Diffusion.sessions()
                .principal("control").password("password").open(serverUrl);

            session2.feature(Messaging.class).setRequestStream(path, String.class, String.class,
                new MyRequestStream2());

            final Session session3 = Diffusion.sessions()
                .principal("control").password("password").open(serverUrl);

            final int result = session3.feature(Messaging.class)
                .sendRequestToFilter("$Principal is 'admin'", path, "Hello", String.class, String.class,
                    new MyFilteredRequestCallback())
                .join();

            LOG.info("Message received by {} session(s).", result);

            SECONDS.sleep(2);

            session2.feature(Messaging.class).removeRequestStream(path);
            session1.feature(Messaging.class).removeRequestStream(path);
            session3.close();
            session2.close();
        }
    }

    public static class MyRequestStream1 implements Messaging.RequestStream<String, String> {
        private static final Logger LOG = LoggerFactory.getLogger(MyRequestStream1.class);

        @Override
        public void onRequest(
            String path,
            String request,
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

    public static class MyRequestStream2 implements Messaging.RequestStream<String, String> {
        private static final Logger LOG = LoggerFactory.getLogger(MyRequestStream2.class);

        @Override
        public void onRequest(
            String path,
            String request,
            Responder<String> responder) {

            LOG.error("Received message: '{}' - this was not expected!!", request);

            responder.respond("I'm not supposed to receive a message.");
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

    public static class MyFilteredRequestCallback implements Messaging.FilteredRequestCallback<String> {
        private static final Logger LOG = LoggerFactory.getLogger(MyFilteredRequestCallback.class);

        @Override
        public void onResponse(
            SessionId sessionId,
            String response) {

            LOG.info("Session: {} responded with: '{}'.", sessionId, response);
        }

        @Override
        public void onResponseError(
            SessionId sessionId,
            Throwable throwable) {

            LOG.error("Session: {} had response error: {}", sessionId, throwable.getMessage());
        }
    }
}
