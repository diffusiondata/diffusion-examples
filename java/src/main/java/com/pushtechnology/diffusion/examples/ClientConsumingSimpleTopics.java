/*******************************************************************************
 * Copyright (C) 2017, 2023 DiffusionData Ltd.
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pushtechnology.diffusion.client.Diffusion;
import com.pushtechnology.diffusion.client.features.Topics;
import com.pushtechnology.diffusion.client.session.Session;
import com.pushtechnology.diffusion.client.topics.details.TopicSpecification;
import com.pushtechnology.diffusion.client.topics.details.TopicType;

/**
 * This demonstrates a client consuming simple scalar topics.
 * <P>
 * All updates will be routed to listeners appropriate for the topic type.
 * <P>
 * This example shows a client consuming a {@link TopicType#STRING STRING} topic
 * but the same technique could be used for {@link TopicType#INT64 INT64} or
 * {@link TopicType#DOUBLE DOUBLE} topics.
 *
 * @author DiffusionData Limited
 * @since 6.0
 * @see ControlClientUpdatingSimpleTopics
 */
public final class ClientConsumingSimpleTopics {

    private static final Logger LOG =
        LoggerFactory.getLogger(ClientConsumingSimpleTopics.class);

    private final Session session;

    /**
     * Constructor.
     *
     * @param serverUrl for example "ws://diffusion.example.com:80
     */
    public ClientConsumingSimpleTopics(
        String serverUrl,
        final StringListener stringListener) {

        session =
            Diffusion.sessions().principal("client").password("password")
                .open(serverUrl);

        // Use the Topics feature to add stream and subscribe to the topic.
        final Topics topics = session.feature(Topics.class);

        topics.addStream(
            "StringTopic",
            String.class,
            new Topics.ValueStream.Default<String>() {
                @Override
                public void onValue(
                    String topicPath,
                    TopicSpecification specification,
                    String oldValue,
                    String newValue) {

                    stringListener.onNewValue(topicPath, newValue);
                }
            });

        topics.subscribe("StringTopic")
            .whenComplete((voidResult, exception) -> {
                if (exception != null) {
                    LOG.info("subscription failed", exception);
                }
            });
    }

    /**
     * Close session.
     */
    public void close() {
        session.close();
    }

    /**
     * A listener for string values.
     */
    public interface StringListener {

        /**
         * Notification of a new String value.
         */
        void onNewValue(String topic, String value);
    }
}
