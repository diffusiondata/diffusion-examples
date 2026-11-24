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

import static java.util.concurrent.TimeUnit.SECONDS;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pushtechnology.diffusion.client.Diffusion;
import com.pushtechnology.diffusion.client.features.control.clients.ClientControl;
import com.pushtechnology.diffusion.client.session.Session;

/**
 * This example demonstrates how to close multiple sessions using a session
 * filter.
 * <P>
 * The example uses the filter "$Principal is 'client'" to ensure the operation
 * only applies to 'client' sessions.
 *
 * @author DiffusionData Limited
 */
public class CloseClientViaSessionFilterExample {

    private static final Logger LOG =
        LoggerFactory.getLogger(CloseClientViaSessionFilterExample.class);

    public static void main(String[] args) throws Exception {

        final Session adminSession = Diffusion.sessions()
            .principal("admin")
            .password("password")
            .open("ws://localhost:8080");

        final Session clientSession1 = Diffusion.sessions()
            .principal("client")
            .password("password")
            .open("ws://localhost:8080");

        final Session clientSession2 = Diffusion.sessions()
            .principal("client")
            .password("password")
            .open("ws://localhost:8080");

        final ClientControl clientControl = adminSession.feature(ClientControl.class);

        clientControl.close("$Principal is 'client'").join();

        SECONDS.sleep(1);

        LOG.info("Client session 1 state: {}", clientSession1.getState());
        LOG.info("Client session 2 state: {}", clientSession2.getState());

        adminSession.close();
    }
}
