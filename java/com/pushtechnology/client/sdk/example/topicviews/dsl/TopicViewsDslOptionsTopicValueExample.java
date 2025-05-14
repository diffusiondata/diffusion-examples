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
import com.pushtechnology.diffusion.client.features.control.topics.views.TopicView;
import com.pushtechnology.diffusion.client.session.Session;
import com.pushtechnology.diffusion.client.topics.details.TopicSpecification;
import com.pushtechnology.diffusion.client.topics.details.TopicType;
import com.pushtechnology.diffusion.datatype.json.JSON;

/**
 * This example demonstrates how to use the topic value transformation in a topic view.
 * <P>
 * A topic view is created that maps a source topic to a new topic, using part of the
 * source topic's value as the new topic's value.
 *
 * @author DiffusionData Limited
 */
public class TopicViewsDslOptionsTopicValueExample {

    private static final Logger LOG =
        LoggerFactory.getLogger(TopicViewsDslOptionsTopicValueExample.class);

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

            topics.addAndSet(
                    "my/topic/path",
                    Diffusion.newTopicSpecification(TopicType.JSON), JSON.class, originalCastJsonValue)
                .join();

            final String topicSelectorExpression = "?views//";

            final MyStream valueStream = new MyStream();
            topics.addStream(topicSelectorExpression, JSON.class, valueStream);
            topics.subscribe(topicSelectorExpression).join();

            final TopicView view = topics.createTopicView("topic_view_1",
                    "map my/topic/path to views/<scalar(/account)> as <value(/balance)>")
                .join();

            LOG.info("Topic View {} has been created.", view.getName());

            SECONDS.sleep(1);
            topics.removeStream(valueStream);
        }
    }

    private static final class MyStream implements Topics.ValueStream<JSON> {

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
