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
package com.pushtechnology.client.sdk.example.sessionmanagement.clientcontrol;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pushtechnology.diffusion.client.Diffusion;
import com.pushtechnology.diffusion.client.features.control.clients.ClientControl;
import com.pushtechnology.diffusion.client.session.Session;
import com.pushtechnology.diffusion.client.session.SessionId;

/**
 * This example demonstrates how to set queue conflation for a session
 * using its ID.
 *
 * @author DiffusionData Limited
 */
public class QueueConflationViaSessionIdExample {

    private static final Logger LOG = LoggerFactory.getLogger(
        QueueConflationViaSessionIdExample.class);

    public static void main(String[] args) throws Exception {

        final Session adminSession = Diffusion.sessions()
            .principal("admin")
            .password("password")
            .open("ws://localhost:8080");

        final Session clientSession = Diffusion.sessions()
            .principal("client")
            .password("password")
            .open("ws://localhost:8080");

        final SessionId clientSessionID = clientSession.getSessionId();

        final ClientControl clientControl = adminSession.feature(ClientControl.class);
        clientControl.setConflated(clientSessionID, false).join();

        LOG.info("Queue conflation configured for session {}.", clientSessionID);

        adminSession.close();
        clientSession.close();
    }
}
