/*******************************************************************************
 * Copyright (C) 2017, 2018 Push Technology Ltd.
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
import static com.pushtechnology.diffusion.client.topics.details.TopicSpecification.REMOVAL;
import static com.pushtechnology.diffusion.client.topics.details.TopicType.STRING;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.concurrent.CompletableFuture;

import com.pushtechnology.diffusion.client.Diffusion;
import com.pushtechnology.diffusion.client.features.TopicUpdate;
import com.pushtechnology.diffusion.client.features.control.topics.TopicControl;
import com.pushtechnology.diffusion.client.session.Session;
import com.pushtechnology.diffusion.client.topics.details.TopicSpecification;

/**
 * An example of using a control client to create and update a simple scalar
 * topic in non exclusive mode (as opposed to acting as an exclusive update
 * source). In this mode other clients could update the same topic (on a last
 * update wins basis).
 * <P>
 * This uses the 'TopicControl' feature to create a topic and the
 * 'TopicUpdate' feature to send updates to it.
 * <P>
 * To send updates to a topic, the client session requires the 'update_topic'
 * permission for that branch of the topic tree.
 *
 * @author Push Technology Limited
 * @since 6.0
 */
public final class ControlClientUpdatingSimpleTopics {

    private static final String TOPIC = "StringTopic";

    private final Session session;

    /**
     * Constructor.
     *
     * @param serverUrl for example "ws://diffusion.example.com:80"
     */
    public ControlClientUpdatingSimpleTopics(String serverUrl) throws Exception {

        session =
            Diffusion.sessions().principal("control").password("password")
                .open(serverUrl);

        final TopicControl topicControl = session.feature(TopicControl.class);

        // Create the topic and request that it is removed when the session
        // closes
        final TopicSpecification specification =
            topicControl.newSpecification(STRING)
                .withProperty(
                    REMOVAL,
                    "when this session closes remove '?" + TOPIC + "//'");

        topicControl.addTopic(TOPIC, specification).get(5, SECONDS);
    }

    /**
     * Update the topic with a string value.
     *
     * @param value the update value
     * @return a CompletableFuture that completes when a response is received
     *         from the server
     */
    public CompletableFuture<?> update(String value) {
        return session.feature(TopicUpdate.class)
            .set(TOPIC, String.class, value);
    }

    /**
     * Close the session.
     */
    public void close() {
        session.close();
    }
}
