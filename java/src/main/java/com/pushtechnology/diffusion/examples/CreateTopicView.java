package com.pushtechnology.diffusion.examples;

/*******************************************************************************
 * Copyright (C) 2021 Push Technology Ltd.
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

import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.Arrays;

import com.pushtechnology.diffusion.client.Diffusion;
import com.pushtechnology.diffusion.client.features.TopicUpdate;
import com.pushtechnology.diffusion.client.features.control.topics.views.TopicViews;
import com.pushtechnology.diffusion.client.session.Session;
import com.pushtechnology.diffusion.client.topics.details.TopicSpecification;
import com.pushtechnology.diffusion.client.topics.details.TopicType;
import com.pushtechnology.diffusion.datatype.json.JSON;

/**
 * An example of creating a topic view.
 *
 * @author Push Technology Limited
 * @since 6.7
 */
 public final class CreateTopicView {

    private static final String ROOT_TOPIC = "Accounts";
    private static final JSON TOPIC_VALUE
        = Diffusion.dataTypes().json().fromJsonString("{\"foo\" : \"bar\" }");
    private static final TopicSpecification TOPIC_SPECIFICATION
        = Diffusion.newTopicSpecification(TopicType.JSON);
    private final Session session;

    /**
     * Constructor.
     * @param serverUrl url for Diffusion server
     */
    public CreateTopicView(String serverUrl) throws Exception {
        session =
            Diffusion.sessions().principal("control").password("password")
                .open(serverUrl);
    }

    /**
     * Create a topic tree on the server.
     * <P>
     * Populate topic tree with JSON data
     */
    private void addTopics() throws Exception {
        //our topic tree has two branches, 'Free' and 'Premium'
        for (String accountType : Arrays.asList("Free", "Premium")) {
            for (int i = 0; i < 10; i++) {
                final String topicPath = String.format("%s/%s/%s-Account-%d",
                    ROOT_TOPIC, accountType, accountType, i);
                session.feature(TopicUpdate.class)
                    .addAndSet(topicPath, TOPIC_SPECIFICATION, JSON.class, TOPIC_VALUE)
                    .get(5, SECONDS);
            }
        }
    }

    /**
     * Create the topic view.
     * <P>
     * @param viewName name of the topic view
     * <P>
     * The path mapping clause is the part after the 'to' keyword. path is a directive, with '2' as its only parameter.
     * The clause will make reference topics for all the topics under Accounts/Premium at Premium-Accounts-Only
     */
    private void createTopicView() {
        final String viewSpecification = "map ?Accounts/Premium// to Premium-Accounts-Only/<path(2)>";
        session.feature(TopicViews.class).createTopicView("premium-view", viewSpecification);
    }

    /**
     * Run the example.
     */
    public void run() throws Exception {
        addTopics();
        createTopicView();
        session.close();
    }
}
