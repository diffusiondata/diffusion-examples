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

package com.pushtechnology.diffusion.tutorials;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.pushtechnology.diffusion.client.Diffusion;
import com.pushtechnology.diffusion.client.features.TopicUpdate;
import com.pushtechnology.diffusion.client.features.Topics;
import com.pushtechnology.diffusion.client.features.control.topics.TopicControl;
import com.pushtechnology.diffusion.client.session.Session;
import com.pushtechnology.diffusion.client.topics.details.TopicSpecification;
import com.pushtechnology.diffusion.client.topics.details.TopicType;
import com.pushtechnology.diffusion.datatype.json.JSON;
import com.pushtechnology.diffusion.datatype.json.JSONDataType;

/**
 * Example of basic publish / subscribe functionality.
 *
 * @author Push Technology Limited
 * @since 6.0
 */
public class PubSubExample {
    public static void main(String... arguments) throws Exception {

        /**
         *  Connect to Diffusion using hostname/port, with supplied credentials
         */
        final Session session = Diffusion.sessions()
                                         .principal("admin")
                                         .password("password")
                                         .open("ws://localhost:8080");

        final Topics topics = session.feature(Topics.class);
        final TopicControl topicControl = session.feature(TopicControl.class);

        /**
         * Subscribe to the "counter" topic and establish a JSON value stream
         */
        topics.addStream("counter", JSON.class, new Topics.ValueStream.Default<JSON>() {
            @Override
            public void onSubscription(String topicPath, TopicSpecification specification) {
                System.out.println("Subscribed to: " + topicPath);
            }

            @Override
            public void onValue(String topicPath, TopicSpecification specification, JSON oldValue, JSON newValue) {
                System.out.println(topicPath + " : " + newValue.toJsonString());
            }
        });

        topics.subscribe("counter");

        /**
         * Add the "counter" topic
         */
        topicControl.addTopic(
            "counter",
            topicControl.newSpecification(TopicType.JSON));

        final JSONDataType jsonDataType = Diffusion.dataTypes().json();
        final TopicUpdate topicUpdate = session.feature(TopicUpdate.class);

        final AtomicInteger i = new AtomicInteger(0);

        /**
         * Schedule a recurring task that increments the counter and updates the "counter" topic with a JSON value
         */
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            final JSON value = jsonDataType.fromJsonString(
                String.format("{\"count\" : %d }", i.getAndIncrement()));

            topicUpdate.set("counter", JSON.class, value);
        }, 1000, 1000, TimeUnit.MILLISECONDS);
    }
}
