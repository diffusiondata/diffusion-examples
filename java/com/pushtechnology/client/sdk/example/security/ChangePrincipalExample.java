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
package com.pushtechnology.client.sdk.example.security;

import com.pushtechnology.diffusion.client.Diffusion;
import com.pushtechnology.diffusion.client.features.Security;
import com.pushtechnology.diffusion.client.session.Session;
import com.pushtechnology.diffusion.client.types.Credentials;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChangePrincipalExample {

    private static final Logger LOG = LoggerFactory.getLogger(
        ChangePrincipalExample.class);

    public static void main(String[] args) throws Exception {

        Credentials credentials = Diffusion.credentials().password("password");

        Session session = Diffusion.sessions()
            .principal("admin")
            .credentials(credentials)
            .open("ws://localhost:8080");

        session.feature(Security.class)
            .changePrincipal("control", credentials)
            .join();

        LOG.info("Principal has been changed to {}", session.getPrincipal());

        session.close();
    }
}
