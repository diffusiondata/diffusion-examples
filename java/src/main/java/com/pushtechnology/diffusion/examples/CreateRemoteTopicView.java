/*******************************************************************************
 * Copyright (C) 2022 Push Technology Ltd.
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
package com.pushtechnology.diffusion.examples;

import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.Arrays;

import com.pushtechnology.diffusion.client.Diffusion;
import com.pushtechnology.diffusion.client.features.TopicUpdate;
import com.pushtechnology.diffusion.client.features.control.RemoteServers;
import com.pushtechnology.diffusion.client.features.control.topics.views.TopicViews;
import com.pushtechnology.diffusion.client.session.Session;
import com.pushtechnology.diffusion.client.topics.details.TopicType;
import com.pushtechnology.diffusion.datatype.json.JSON;
import com.pushtechnology.diffusion.datatype.json.JSONDataType;

/**
 * An example of creating a remote topic view.
 * <P>
 * Two sessions are established and a remote topic view is created to map part
 * of the topic tree from the primary server to the secondary server
 *
 * @author Push Technology Limited
 * @since 6.5
 */
public final class CreateRemoteTopicView {

    private static final String ROOT_TOPIC = "Accounts";
    private final String primaryServerUrl;
    private final String secondaryServerUrl;

    /**
     * Constructor.
     *
     * @param primaryServerUrl url for primary server
     * @param secondaryServerUrl url for secondary server
     */
    public CreateRemoteTopicView(
        String primaryServerUrl,
        String secondaryServerUrl) {
        this.primaryServerUrl = primaryServerUrl;
        this.secondaryServerUrl = secondaryServerUrl;
    }

    /**
     * Creates a topic tree on primary server.
     * <P>
     * Populates topic tree with JSON data
     */
    private void addTopics(Session session)
        throws Exception {

        final JSONDataType jsonDataType = Diffusion.dataTypes().json();
        final JSON value = jsonDataType.fromJsonString("{\"foo\" : \"bar\" }");

        for (String type : Arrays.asList("Free", "Premium")) {
            for (int i = 0; i < 10; i++) {
                session.feature(TopicUpdate.class).addAndSet(
                    String.format(
                        "%s/%s/%s-Account-%d",
                        ROOT_TOPIC,
                        type,
                        type,
                        i),
                    Diffusion.newTopicSpecification(TopicType.JSON),
                    JSON.class,
                    value).get(5, SECONDS);
            }
        }
    }

    /**
     * Establish remote server.
     *
     * @param name name of the remote server
     */
    private void createRemoteServer(Session session, String name)
        throws Exception {

        session.feature(RemoteServers.class).createRemoteServer(
            Diffusion.newRemoteServerBuilder()
                .principal("admin")
                .credentials(Diffusion.credentials().password("password"))
                .create(name, primaryServerUrl))
            .get(5, SECONDS);

    }

    /**
     * Create the remote topic view.
     *
     * @param serverName name of the remote server
     * @param viewName name of the remote view
     */
    private void createRemoteTopicView(
        Session session,
        String serverName,
        String viewName)
        throws Exception {

        final String specification =
            String.format(
                "map ?%s/%s// from %s to %s/<path(2)>",
                ROOT_TOPIC,
                "Premium",
                serverName,
                viewName);

        session.feature(TopicViews.class).createTopicView(
            viewName,
            specification).get(5, SECONDS);
    }

    /**
     * Run the example.
     */
    public void run() throws Exception {

        final Session primarySession =
            Diffusion.sessions().principal("admin").password("password")
                .open(primaryServerUrl);

        final Session secondarySession =
            Diffusion.sessions().principal("admin").password("password")
                .open(secondaryServerUrl);

        addTopics(primarySession);
        createRemoteServer(secondarySession, "foo");
        createRemoteTopicView(secondarySession, "foo", "bar");

        primarySession.close();
        secondarySession.close();
    }
}
