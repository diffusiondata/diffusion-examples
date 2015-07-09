/*******************************************************************************
 * Copyright (C) 2014, 2015 Push Technology Ltd.
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
import com.pushtechnology.diffusion.client.content.Content;
import com.pushtechnology.diffusion.client.content.RecordContentReader;
import com.pushtechnology.diffusion.client.features.Topics;
import com.pushtechnology.diffusion.client.features.Topics.TopicStream;
import com.pushtechnology.diffusion.client.session.Session;
import com.pushtechnology.diffusion.client.types.UpdateContext;

/**
 * In this simple and commonest case for a client we just subscribe to a few
 * topics and assign handlers for each to receive content.
 * <P>
 * This makes use of the 'Topics' feature only.
 * <P>
 * To subscribe to a topic, the client session must have the 'read_topic'
 * permission for that branch of the topic tree.
 *
 * @author Push Technology Limited
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
        topics.addTopicStream(">Foo", new FooTopicStream());
        topics.addTopicStream(">Bar/", new BarTopicStream());
        topics.subscribe(
            Diffusion.topicSelectors().anyOf("Foo", "Bar//"),
            new Topics.CompletionCallback.Default());
    }

    /**
     * Close session.
     */
    public void close() {
        session.close();
    }

    /**
     * The topic stream for all messages on the 'Foo' topic.
     */
    private class FooTopicStream extends TopicStream.Default {
        @Override
        public void onTopicUpdate(
            String topic,
            Content content,
            UpdateContext context) {

            LOG.info(content.asString());
        }
    }

    /**
     * The topic stream for all messages on 'Bar' topics.
     */
    private class BarTopicStream extends TopicStream.Default {
        @Override
        public void onTopicUpdate(
            String topic,
            Content content,
            UpdateContext context) {

            // Process the message - one with a record with a variable number of
            // fields followed by two more fields (effectively another record
            // but no need to process as such).

            final RecordContentReader reader =
                Diffusion.content().newReader(
                    RecordContentReader.class,
                    content);

            for (String field : reader.nextRecord()) {
                LOG.info("Record 1 Field={}", field);
            }

            LOG.info("Extra Field 1={}", reader.nextField());
            LOG.info("Extra Field 2={}", reader.nextField());
        }
    }
}
