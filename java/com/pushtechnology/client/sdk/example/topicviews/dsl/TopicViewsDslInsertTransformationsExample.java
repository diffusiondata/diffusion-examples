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

import com.pushtechnology.diffusion.client.Diffusion;
import com.pushtechnology.diffusion.client.callbacks.ErrorReason;
import com.pushtechnology.diffusion.client.features.Topics;
import com.pushtechnology.diffusion.client.session.Session;
import com.pushtechnology.diffusion.client.topics.details.TopicSpecification;
import com.pushtechnology.diffusion.client.topics.details.TopicType;
import com.pushtechnology.diffusion.datatype.json.JSON;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TopicViewsDslInsertTransformationsExample {

    private static final Logger LOG =
        LoggerFactory.getLogger(TopicViewsDslInsertTransformationsExample.class);

    public static void main(String[] args)
        throws Exception {

        try (Session session = Diffusion.sessions()
            .principal("admin")
            .password("password")
            .open("ws://localhost:8080")) {

            final Topics topics = session.feature(Topics.class);
            final TopicSpecification specification = Diffusion.newTopicSpecification(TopicType.JSON);
            final String topicSelectorExpression = "?views//";
            final MyLoggingJsonStream myLoggingJsonStream = new MyLoggingJsonStream();
            final String topicViewName = "topic_view_1";

            final JSON originalCastJsonValue = Diffusion.dataTypes().json().fromJsonString("[\n" +
                "  \"Fred Flintstone\"," +
                "  \"Wilma Flintstone\"," +
                "  \"Barney Rubble\"," +
                "  \"Betty Rubble\"" +
                "]");

            final JSON additionalCast1 = Diffusion.dataTypes().json()
                .fromJsonString("[\"Pebbles Flintstone\"]");

            final JSON additionalCast2 = Diffusion.dataTypes().json()
                .fromJsonString("[\"Bamm-Bamm Rubble\"]");

            topics.addAndSet(
                    "my/topic/path/original_cast", specification, JSON.class, originalCastJsonValue)
                .join();

            topics.addAndSet(
                    "my/topic/path/additional_cast/1", specification, JSON.class, additionalCast1)
                .join();

            topics.addAndSet(
                    "my/topic/path/additional_cast/2", specification, JSON.class, additionalCast2)
                .join();

            topics.addFallbackStream(JSON.class, myLoggingJsonStream);
            topics.subscribe(topicSelectorExpression).join();

            topics.createTopicView(topicViewName,
                    "map my/topic/path/original_cast\n" +
                        "  to views/the_flintstones\n" +
                        "  insert my/topic/path/additional_cast/1 at /-\n" +
                        "  insert my/topic/path/additional_cast/2 at /-")
                .join();

            LOG.info("Topic View {} has been created.", topicViewName);

            topics.unsubscribe(topicSelectorExpression)
                .join();

            topics.removeStream(myLoggingJsonStream);

            SECONDS.sleep(1);
        }
    }

    public static class MyLoggingJsonStream implements Topics.ValueStream<JSON> {
        private static final Logger LOG = LoggerFactory.getLogger(MyLoggingJsonStream.class);

        @Override
        public void onValue(
            String topicPath,
            TopicSpecification topicSpecification,
            JSON oldValue,
            JSON newValue) {

            LOG.info("'{}' changed from '{}' to '{}}'.",
                topicPath, oldValue, newValue);
        }

        @Override
        public void onSubscription(
            String topicPath,
            TopicSpecification topicSpecification) {

            LOG.info("Subscribed to: '{}'.", topicPath);
        }

        @Override
        public void onUnsubscription(
            String topicPath,
            TopicSpecification topicSpecification,
            Topics.UnsubscribeReason unsubscribeReason) {

            LOG.info("Unsubscribed from: '{}', reason: {}.",
                topicPath, unsubscribeReason);
        }

        @Override
        public void onClose() {
            LOG.info("On close.");
        }

        @Override
        public void onError(ErrorReason errorReason) {
            LOG.error("On error: {}.", errorReason);
        }
    }
}
