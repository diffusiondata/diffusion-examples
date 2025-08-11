/*******************************************************************************
 * Copyright (C) 2025 DiffusionData Ltd.
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
package com.pushtechnology.client.sdk.example.security;

import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pushtechnology.diffusion.client.Diffusion;
import com.pushtechnology.diffusion.client.features.Security;
import com.pushtechnology.diffusion.client.session.Session;
import com.pushtechnology.diffusion.client.types.Credentials;

/**
 * This example demonstrates how to re-authenticate a Diffusion session.
 *
 * @author DiffusionData Limited
 */
public class ReauthenticateExample {

    private static final Logger LOG = LoggerFactory.getLogger(
        ReauthenticateExample.class);

    public static void main(String[] args) throws Exception {

        final Credentials credentials = Diffusion.credentials().password("password");

        final Session session = Diffusion.sessions()
            .principal("admin")
            .credentials(credentials)
            .open("ws://localhost:8080");

        session.feature(Security.class)
            .reauthenticate("control", credentials, Collections.emptyMap())
            .join();

        LOG.info("Session reauthenticated for principal {}", session.getPrincipal());

        session.close();
    }
}
