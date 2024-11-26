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
package com.pushtechnology.client.sdk.example.remoteservers;

import com.pushtechnology.diffusion.client.Diffusion;
import com.pushtechnology.diffusion.client.features.control.RemoteServers;
import com.pushtechnology.diffusion.client.features.control.RemoteServers.PrimaryInitiator.PrimaryInitiatorBuilder;
import com.pushtechnology.diffusion.client.session.Session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class CreatePrimaryInitiatorExample {

    private static final Logger LOG = LoggerFactory.getLogger(
        CreatePrimaryInitiatorExample.class);

    public static void main(String[] args) {

        Session adminSession = Diffusion.sessions()
            .principal("admin")
            .password("password")
            .open("ws://localhost:8080");

        RemoteServers remoteServersControl =
            adminSession.feature(RemoteServers.class);

        RemoteServers.PrimaryInitiator.PrimaryInitiatorBuilder builder =
            Diffusion.newRemoteServerBuilder(PrimaryInitiatorBuilder.class);

        List<String> urls = new ArrayList<String>(){{
            add("ws://new.server.url.com:8080");
            add("ws://new.server.url.com:8081");
            add("ws://new.server.url.com:8082");
        }};

        remoteServersControl.createRemoteServer(
            builder
                .retryDelay(2500)
                .build("Remote Server 1", urls, "High Volume Connector")
        ).join();

        remoteServersControl.removeRemoteServer("Remote Server 1").join();
        adminSession.close();

        LOG.info("created remote server");
    }
}
