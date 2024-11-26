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

import com.pushtechnology.diffusion.client.Diffusion;
import com.pushtechnology.diffusion.client.features.control.clients.ClientControl;
import com.pushtechnology.diffusion.client.features.control.clients.ClientControl.SessionFilterOperationResult;
import com.pushtechnology.diffusion.client.session.Session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Map;

public class SetSessionPropertiesViaFilterExample {
    private static final Logger LOG = LoggerFactory.getLogger(
        SetSessionPropertiesViaFilterExample.class);

    public static void main(String[] args) throws Exception {

        Session adminSession = Diffusion.sessions()
            .principal("admin")
            .password("password")
            .open("ws://localhost:8080");

        Session clientSession = Diffusion.sessions()
            .principal("client")
            .password("password")
            .open("ws://localhost:8080");

        ClientControl clientControl = adminSession.feature(ClientControl.class);

        Map<String, String> sessionProperties = clientControl
            .getSessionProperties(clientSession.getSessionId(), Collections.singleton(Session.ALL_FIXED_PROPERTIES)).join();

        sessionProperties.forEach((key, value) -> System.out.printf("  <%s> : <%s>\n", key, value));

        Map<String, String> myProperties = Collections.singletonMap("$Country", "CA");

        SessionFilterOperationResult result = clientControl
            .setSessionProperties("$Principal is 'client'", myProperties)
            .join();

        System.out.printf("Updated %s session(s)\n", result.selected());

        sessionProperties = clientControl
            .getSessionProperties(clientSession.getSessionId(), Collections.singleton(Session.ALL_FIXED_PROPERTIES)).join();

        sessionProperties.forEach((key, value) -> System.out.printf("  <%s> : <%s>\n", key, value));

        adminSession.close();
        clientSession.close();

        LOG.info("Updated {} session(s)", result.selected());
    }
}
