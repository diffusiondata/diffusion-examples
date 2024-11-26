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
package com.pushtechnology.client.sdk.example.monitoring;

import java.util.Collections;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pushtechnology.diffusion.client.Diffusion;
import com.pushtechnology.diffusion.client.callbacks.ErrorReason;
import com.pushtechnology.diffusion.client.callbacks.Registration;
import com.pushtechnology.diffusion.client.features.control.clients.ClientControl;
import com.pushtechnology.diffusion.client.features.control.clients.ClientControl.SessionEventParameters;
import com.pushtechnology.diffusion.client.features.control.clients.ClientControl.SessionEventStream.Event.Type;
import com.pushtechnology.diffusion.client.session.Session;

public class SessionEventListenerExample {

    public static void main(String[] args) throws Exception {

        final Session session1 = Diffusion.sessions()
            .principal("admin")
            .password("password")
            .open("ws://localhost:8080");

        final Session session2 = Diffusion.sessions()
            .principal("client")
            .password("password")
            .open("ws://localhost:8080");

        final ClientControl clientControl = session1.feature(ClientControl.class);

        // We specify the session properties to be returned and we
        // exclude sessions with the 'admin' principal
        final SessionEventParameters parameters =
            Diffusion.newSessionEventParametersBuilder()
                .properties(Session.ALL_FIXED_PROPERTIES)
                .filter("$Principal NE 'admin'")
                .build();

        final Registration registration = clientControl.addSessionEventListener(
            new MyEventStream(), parameters).join();

        clientControl.setSessionProperties(session2.getSessionId(),
            Collections.singletonMap("$Country", "CA")).join();

        session2.close();
        registration.close();

        Thread.sleep(2000);

        session1.close();
    }

    /**
     * Define an implementation of SessionEventSteam with our desired behaviour.
     */
    static class MyEventStream implements ClientControl.SessionEventStream {

        private static final Logger LOG =
            LoggerFactory.getLogger(MyEventStream.class);

        @Override
        public void onSessionEvent(Event event) {

            if (event.isOpenEvent()) {
                LOG.info("New session: {}", event.sessionId());
                return;
            }

            if (event.type() == Type.STATE) {
                LOG.info("Session state changed {}", event.state());

            }
            else {
                event.changedProperties().forEach((property, value) -> {
                    LOG.info("Session property changed: {}={}", property, value);
                });
            }
        }

        @Override
        public void onClose() {
            LOG.info("Stream closed");
        }

        @Override
        public void onError(ErrorReason errorReason) {
            LOG.error("An error occured: {}",
                errorReason.getDescription());
        }
    }
}
