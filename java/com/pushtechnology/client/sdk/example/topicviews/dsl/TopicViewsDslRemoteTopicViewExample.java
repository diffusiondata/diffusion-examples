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
package com.pushtechnology.client.sdk.example.topicviews.dsl;

import com.pushtechnology.diffusion.client.Diffusion;
import com.pushtechnology.diffusion.client.features.control.RemoteServers;
import com.pushtechnology.diffusion.client.features.control.topics.views.TopicViews;
import com.pushtechnology.diffusion.client.session.Session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TopicViewsDslRemoteTopicViewExample {
    private static final Logger LOG = LoggerFactory.getLogger(TopicViewsDslRemoteTopicViewExample.class);

    public static void main(String[] args)
        throws Exception {

        try (Session session = Diffusion.sessions()
            .principal("admin")
            .password("password")
            .open("ws://localhost:8080")) {

            final String remoteTopicViewName = "remote_topic_view_1";

            final RemoteServers.RemoteServer remoteServer =
                Diffusion.newRemoteServerBuilder(RemoteServers.SecondaryInitiator.SecondaryInitiatorBuilder.class)
                    .principal("admin").credentials(Diffusion.credentials().password("password"))
                    .connectionOption(RemoteServers.RemoteServer.ConnectionOption.RECONNECTION_TIMEOUT, "120000")
                    .connectionOption(RemoteServers.RemoteServer.ConnectionOption.MAXIMUM_QUEUE_SIZE, "1000")
                    .connectionOption(RemoteServers.RemoteServer.ConnectionOption.CONNECTION_TIMEOUT, "15000")
                    .build("Remote Server 1", "ws://new.server.url.com");

            session.feature(RemoteServers.class).createRemoteServer(remoteServer)
                .join();

            session.feature(TopicViews.class).createTopicView(
                    remoteTopicViewName,
                    "map my/topic/path from 'Remote Server 1' to views/remote/<path(0)>")
                .join();

            LOG.info("Remote Topic View '{}' has been created.", remoteTopicViewName);
        }
    }
}
