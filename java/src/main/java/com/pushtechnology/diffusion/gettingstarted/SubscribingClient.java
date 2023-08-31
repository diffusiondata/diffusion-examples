/*******************************************************************************
 * Copyright (C) 2014, 2023 DiffusionData Ltd.
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
package com.pushtechnology.diffusion.gettingstarted;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pushtechnology.diffusion.client.Diffusion;
import com.pushtechnology.diffusion.client.features.Topics;
import com.pushtechnology.diffusion.client.features.Topics.ValueStream;
import com.pushtechnology.diffusion.client.session.Session;
import com.pushtechnology.diffusion.client.topics.details.TopicSpecification;

/**
 * A client that subscribes to the topic 'foo/counter.
 *
 * @author DiffusionData Limited
 * @since 5.5
 */
public class SubscribingClient {

    private static final Logger LOG =
        LoggerFactory.getLogger(SubscribingClient.class);

    /**
     * Main.
     */
    public static void main(String... arguments) throws Exception {

        // Connect anonymously
        // Replace 'host' with your hostname
        final Session session = Diffusion.sessions().open("ws://host:80");

        // Get the Topics feature to subscribe to topics
        final Topics topics = session.feature(Topics.class);

        // Add a new stream for 'foo/counter'
        topics.addStream(">foo/counter", Long.class, new ValueStreamPrintLn());

        // Subscribe to the topic 'foo/counter'
        topics.subscribe("foo/counter")
            .whenComplete((voidResult, exception) -> {
                if (exception != null) {
                    LOG.info("subscription failed", exception);
                }
            });

        // Wait for a minute while the stream prints updates
        Thread.sleep(60000);
    }

    /**
     * A topic stream that prints updates to the console.
     */
    private static class ValueStreamPrintLn extends ValueStream.Default<Long> {
        @Override
        public void onValue(
            String topicPath,
            TopicSpecification specification,
            Long oldValue,
            Long newValue) {
            System.out.println(topicPath + ":   " + newValue);
        }
    }
}