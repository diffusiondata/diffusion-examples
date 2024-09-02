/*******************************************************************************
 * Copyright (C) 2024 DiffusionData Ltd.
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

import java.time.Instant;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pushtechnology.diffusion.client.Diffusion;
import com.pushtechnology.diffusion.client.callbacks.ErrorReason;
import com.pushtechnology.diffusion.client.callbacks.Registration;
import com.pushtechnology.diffusion.client.features.control.clients.ClientControl;
import com.pushtechnology.diffusion.client.features.control.clients.ClientControl.SessionEventParameters;
import com.pushtechnology.diffusion.client.features.control.clients.ClientControl.SessionEventStream.Event.Type;
import com.pushtechnology.diffusion.client.session.Session;

/**
 * An example of a client using a SessionEventListener to receive notifications
 * of session events.
 *
 * @author DiffusionData Limited
 * @since 6.11
 */
public class ClientUsingSessionEventListener {

    private static final Logger LOG =
        LoggerFactory.getLogger(ClientUsingSessionEventListener.class);
    private final Session session;
    private final Registration registration;


    /**
     * Constructor.
     */
    public ClientUsingSessionEventListener() throws Exception {
        session = Diffusion.sessions()
            .principal("control")
            .password("password")
            .open("ws://localhost:8080");


        /**
         * Parameters for our listener. We specify two session properties
         * to be returned, we exclude sessions with the 'admin' principal,
         * and we ignore old events.
         */
        final SessionEventParameters parameters = Diffusion.newSessionEventParametersBuilder()
            .properties("$Principal", "$COUNTRY ")
            .filter("$Principal NE 'admin'")
            .after(Instant.now())
            .build();

        // Register the event listener
        registration = session.feature(ClientControl.class)
            .addSessionEventListener(new MyEventStream(), parameters).get(5, TimeUnit.SECONDS);
    }

    /**
     * Close the session.
     */
    public void close() {
        registration.close();
        session.close();
    }

    /**
     * Define an implementation of SessionEventSteam with our desired
     * behaviour.
     */
    class MyEventStream implements ClientControl.SessionEventStream {

        @Override
        public void onSessionEvent(Event event) {
            if (event.isOpenEvent()) {
                LOG.info("New session: id=%s", event.sessionId());
                return;
            }

            if (event.type() == Type.STATE) {
                LOG.info("Session state changed: id=%s, state=%s",
                    event.sessionId(),
                    event.state());
            }
            else {
                LOG.info("Session properties changed: id=%s, properties=%s",
                    event.sessionId(),
                    event.changedProperties());
            }
        }

        @Override
        public void onClose() {
            LOG.info("Stream closed");
        }

        @Override
        public void onError(ErrorReason errorReason) {
            LOG.info("An error occured: %s", errorReason.getDescription());
        }
    }
}