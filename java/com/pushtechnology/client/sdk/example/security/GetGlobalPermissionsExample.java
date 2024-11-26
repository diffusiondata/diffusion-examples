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
import com.pushtechnology.diffusion.client.types.GlobalPermission;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

public class GetGlobalPermissionsExample {

    private static final Logger LOG = LoggerFactory.getLogger(
        GetGlobalPermissionsExample.class);

    public static void main(String[] args) throws Exception {

        Session session = Diffusion.sessions()
            .principal("admin")
            .password("password")
            .open("ws://localhost:8080");

        Set<GlobalPermission> globalPermissions = session.feature(Security.class)
            .getGlobalPermissions()
            .join();

        for (GlobalPermission permission : globalPermissions) {
            LOG.info(permission.name());
        }

        session.close();
    }
}
