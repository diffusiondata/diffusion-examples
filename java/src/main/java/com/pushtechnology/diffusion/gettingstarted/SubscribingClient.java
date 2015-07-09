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
package com.pushtechnology.diffusion.gettingstarted;

import com.pushtechnology.diffusion.client.Diffusion;
import com.pushtechnology.diffusion.client.content.Content;
import com.pushtechnology.diffusion.client.features.Topics;
import com.pushtechnology.diffusion.client.features.Topics.TopicStream;
import com.pushtechnology.diffusion.client.session.Session;
import com.pushtechnology.diffusion.client.types.UpdateContext;

/**
 * A client that publishes an incrementing count to the topic 'foo/counter.
 *
 * @author Push Technology Limited
 * @since 5.5
 */
public class SubscribingClient {

    /**
     * Main.
     */
    public static void main(String... arguments) throws Exception {

        // Connect anonymously
        // Replace 'host' with your hostname
        final Session session = Diffusion.sessions().open("ws://host:80");

        // Get the Topics feature to subscribe to topics
        final Topics topics = session.feature(Topics.class);

        // Add a new topic stream for 'foo/counter'
        topics.addTopicStream(">foo/counter", new TopicStreamPrintLn());

        // Subscribe to the topic 'foo/counter'
        topics.subscribe("foo/counter",
                new Topics.CompletionCallback.Default());

        // Wait for a minute while the stream prints updates
        Thread.sleep(60000);
    }

    /**
     * A topic stream that prints updates to the console.
     */
    private static class TopicStreamPrintLn extends TopicStream.Default {
        @Override
        public void onTopicUpdate(String topic, Content content,
                UpdateContext context) {

            System.out.println(topic + ":   " + content.asString());
        }
    }
}