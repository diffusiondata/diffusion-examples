/*******************************************************************************
 * Copyright (C) 2014, 2018 Push Technology Ltd.
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
import com.pushtechnology.diffusion.client.features.Topics.ValueStream;
import com.pushtechnology.diffusion.client.session.Session;
import com.pushtechnology.diffusion.client.topics.details.TopicSpecification;
import com.pushtechnology.diffusion.datatype.json.JSON;

/**
 * In this simple and commonest case for a client we just subscribe to a few
 * topics and assign handlers for each to receive content.
 * <P>
 * This makes use of the 'Topics' feature only.
 * <P>
 * To subscribe to a topic, the client session must have the
 * 'select_topic' and 'read_topic' permissions for that branch of the
 * topic tree.
 *
 * @author DiffusionData Limited
 * @since 5.0
 */
public final class ClientSimpleSubscriber {

    private static final Logger LOG =
        LoggerFactory.getLogger(ClientSimpleSubscriber.class);

    private final Session session;

    /**
     * Constructor.
     */
    public ClientSimpleSubscriber() {

        session =
            Diffusion.sessions().principal("client").password("password")
                .open("ws://diffusion.example.com:80");

        // Use the Topics feature to add a topic stream for
        // Foo and all topics under Bar and request subscription to those topics
        final Topics topics = session.feature(Topics.class);
        topics.addStream(">Foo", String.class, new FooStream());
        topics.addStream(">Bar//", JSON.class, new BarStream());

        topics.subscribe(Diffusion.topicSelectors().anyOf("Foo", "Bar//"))
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
     * The stream for all messages on the 'Foo' String topic.
     */
    private class FooStream extends ValueStream.Default<String> {
        @Override
        public void onValue(
            String topicPath,
            TopicSpecification specification,
            String oldValue,
            String newValue) {
            LOG.info(newValue);
        }
    }

    /**
     * The stream for all messages on 'Bar' topics which are JSON topics.
     */
    private class BarStream extends ValueStream.Default<JSON> {
        @Override
        public void onValue(
            String topicPath,
            TopicSpecification specification,
            JSON oldValue,
            JSON newValue) {
            LOG.info(newValue.toJsonString());
        }
    }
}
