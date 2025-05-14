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
package com.pushtechnology.client.sdk.example.topicviews.dsl;

import static java.util.concurrent.TimeUnit.SECONDS;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pushtechnology.diffusion.client.Diffusion;
import com.pushtechnology.diffusion.client.callbacks.ErrorReason;
import com.pushtechnology.diffusion.client.features.Topics;
import com.pushtechnology.diffusion.client.session.Session;
import com.pushtechnology.diffusion.client.topics.details.TopicSpecification;
import com.pushtechnology.diffusion.client.topics.details.TopicType;
import com.pushtechnology.diffusion.datatype.json.JSON;

/**
 * This example demonstrates how to use the source path directive in a topic view.
 * <P>
 * A JSON topic is created at a deep path. The source path directive is used to extract
 * different segments of the path to dynamically generate new topic paths.
 *
 * @author DiffusionData Limited
 */
public class TopicViewsDslSourcePathDirectiveExample {

    private static final Logger LOG =
        LoggerFactory.getLogger(TopicViewsDslSourcePathDirectiveExample.class);

    public static void main(String[] args) throws Exception {

        try (Session session = Diffusion.sessions()
            .principal("admin")
            .password("password")
            .open("ws://localhost:8080")) {

            final JSON originalCastJsonValue = Diffusion.dataTypes().json().fromJsonString("{\n" +
                "  \"account\": \"1234\"," +
                "  \"balance\": {" +
                "    \"amount\": 12.57," +
                "    \"currency\": \"USD\"" +
                "  }" +
                "}");

            final Topics topics = session.feature(Topics.class);
            final String viewSelector = "?views//";
            final Topics.ValueStream<JSON> valueStream = new MyStream();

            topics.addAndSet(
                    "a/b/c/d/e/f/g",
                    Diffusion.newTopicSpecification(TopicType.JSON),
                    JSON.class, originalCastJsonValue)
                .join();

            topics.addStream(viewSelector, JSON.class, valueStream);
            topics.subscribe(viewSelector).join();

            final String[] topicViewExpressionMappings = new String[]{
                "map a/b/c/d/e/f/g to views/<path(0)>",
                "map a/b/c/d/e/f/g to views/<path(2)>",
                "map a/b/c/d/e/f/g to views/<path(3,5)>"
            };

            for (int i = 0; i < topicViewExpressionMappings.length; i++) {
                final String topicViewName = "topic_view_" + (i + 1);
                final String topicViewExpressionMapping = topicViewExpressionMappings[i];

                topics.createTopicView(topicViewName,
                        topicViewExpressionMapping)
                    .join();

                LOG.info("Topic View {} has been created.", topicViewName);
            }

            topics.removeStream(valueStream);
            SECONDS.sleep(1);
        }
    }

    static class MyStream implements Topics.ValueStream<JSON> {

        @Override
        public void onValue(
            String topicPath,
            TopicSpecification topicSpecification,
            JSON oldValue,
            JSON newValue) {
            LOG.info("{} new value {}", topicPath, newValue.toJsonString());
        }

        @Override
        public void onSubscription(String topicPath,
            TopicSpecification topicSpecification) {
            LOG.info("Subscribed to {}", topicPath);
        }

        @Override
        public void onUnsubscription(String topicPath,
            TopicSpecification topicSpecification,
            Topics.UnsubscribeReason unsubscribeReason) {
            LOG.info("Unsubscribed from {}", topicPath);
        }

        @Override
        public void onClose() {
            LOG.info("stream closed");
        }

        @Override
        public void onError(ErrorReason errorReason) {
            LOG.error("stream error: {}", errorReason);
        }
    }
}
