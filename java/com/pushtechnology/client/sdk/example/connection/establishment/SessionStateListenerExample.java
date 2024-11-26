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
package com.pushtechnology.client.sdk.example.connection.establishment;

import com.pushtechnology.diffusion.client.Diffusion;
import com.pushtechnology.diffusion.client.session.Session;
import com.pushtechnology.diffusion.client.session.Session.Listener;
import com.pushtechnology.diffusion.client.session.Session.State;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SessionStateListenerExample {

    private static final Logger LOG = LoggerFactory
        .getLogger(SessionStateListenerExample.class);

    public static void main(String[] args) {

        final Listener mySessionListener = new Listener() {
            @Override
            public void onSessionStateChanged(Session session, State oldState, State newState) {
                LOG.info("State changed from {} to {}", oldState, newState);
            }
        };

        final Session session = Diffusion.sessions()
            .principal("admin")
            .password("password")
            .listener(mySessionListener)
            .open("ws://localhost:8080");

        // Insert work here

        session.close();
    }
}
