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
package com.pushtechnology.client.sdk.example.serverconfiguration.securitycontrol;

import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pushtechnology.diffusion.client.Diffusion;
import com.pushtechnology.diffusion.client.features.control.clients.SecurityControl;
import com.pushtechnology.diffusion.client.features.control.clients.SecurityControl.ScriptBuilder;
import com.pushtechnology.diffusion.client.session.Session;
import com.pushtechnology.diffusion.client.types.PathPermission;

/**
 * This example demonstrates how to set path-specific permissions for a role using
 * the security control feature in Diffusion.
 * <P>
 * The example grants the `CLIENT` role the `UPDATE_TOPIC` and `MODIFY_TOPIC`
 * permissions for the given path.
 *
 * @author DiffusionData Limited
 */
public class SetPathPermissionsExample {

    private static final Logger LOG = LoggerFactory.getLogger(
        SetPathPermissionsExample.class);

    public static void main(String[] args) throws Exception {

        final Session session = Diffusion.sessions()
            .principal("admin")
            .password("password")
            .open("ws://localhost:8080");

        final SecurityControl securityControl = session.feature(SecurityControl.class);
        final ScriptBuilder builder = securityControl.scriptBuilder();

        final Set<PathPermission> myPermissions = new HashSet<>();
        myPermissions.add(PathPermission.UPDATE_TOPIC);
        myPermissions.add(PathPermission.MODIFY_TOPIC);

        builder.setPathPermissions("CLIENT", "my/topic/path", myPermissions);
        final String script = builder.script();

        LOG.info("Allowing Role CLIENT to update and modify my/topic/path");

        securityControl.updateStore(script)
            .whenComplete((r, ex) -> LOG.info(script));

        session.close();
    }
}
