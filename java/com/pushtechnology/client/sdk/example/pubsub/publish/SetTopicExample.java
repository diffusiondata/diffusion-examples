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
package com.pushtechnology.client.sdk.example.pubsub.publish;

import com.pushtechnology.diffusion.client.Diffusion;
import com.pushtechnology.diffusion.client.features.Topics;
import com.pushtechnology.diffusion.client.features.control.topics.TopicControl;
import com.pushtechnology.diffusion.client.session.Session;
import com.pushtechnology.diffusion.client.topics.details.TopicType;
import com.pushtechnology.diffusion.datatype.json.JSON;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SetTopicExample {

    private static final Logger LOG =
        LoggerFactory.getLogger(SetTopicExample.class);

    public static void main(String[] args) {

        try (Session session = Diffusion.sessions()
            .principal("admin")
            .password("password")
            .open("ws://localhost:8080")) {

            final String topicPath = "my/topic/path";

            final JSON data = Diffusion.dataTypes().json()
                .fromJsonString("{ \"diffusion\": [\"data\", \"more data\"] }");

            final TopicControl.AddTopicResult addTopicResult =
                session.feature(TopicControl.class)
                    .addTopic(topicPath, Diffusion.newTopicSpecification(TopicType.JSON))
                    .join();

            LOG.info("Topic: {}.", addTopicResult);

            session.feature(Topics.class).set(topicPath, JSON.class, data)
                .join();

            LOG.info("Value set.");
        }
    }
}
