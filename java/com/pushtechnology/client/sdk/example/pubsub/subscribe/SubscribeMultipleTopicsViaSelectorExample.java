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
package com.pushtechnology.client.sdk.example.pubsub.subscribe;

import static java.util.concurrent.TimeUnit.SECONDS;

import com.pushtechnology.diffusion.client.Diffusion;
import com.pushtechnology.diffusion.client.callbacks.ErrorReason;
import com.pushtechnology.diffusion.client.features.Topics;
import com.pushtechnology.diffusion.client.features.control.topics.TopicControl;
import com.pushtechnology.diffusion.client.session.Session;
import com.pushtechnology.diffusion.client.topics.details.TopicSpecification;
import com.pushtechnology.diffusion.client.topics.details.TopicType;
import com.pushtechnology.diffusion.datatype.json.JSON;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SubscribeMultipleTopicsViaSelectorExample {

    private static final Logger LOG =
        LoggerFactory.getLogger(SubscribeMultipleTopicsViaSelectorExample.class);

    public static void main(String[] args)
        throws Exception {

        try (Session session = Diffusion.sessions()
            .principal("admin")
            .password("password")
            .open("ws://localhost:8080")) {

            final TopicSpecification topicSpecification = Diffusion.newTopicSpecification(TopicType.JSON);
            final TopicControl topicControl = session.feature(TopicControl.class);

            topicControl.addTopic("my/topic/path", topicSpecification)
                .join();

            topicControl.addTopic("my/other/topic/path", topicSpecification)
                .join();

            final Topics topics = session.feature(Topics.class);
            final MyLoggingJsonStream valueStream = new MyLoggingJsonStream();

            final String topicSelectorExpression = "?my//";

            topics.addStream(topicSelectorExpression, JSON.class, valueStream);
            topics.subscribe(topicSelectorExpression)
                .join();

            final TopicControl.AddTopicResult result =
                topicControl.addTopic("my/additional/topic/path", topicSpecification)
                    .join();

            LOG.info("Third topic: {}.", result);

            topics.unsubscribe(topicSelectorExpression)
                .join();

            topics.removeStream(valueStream);

            SECONDS.sleep(2); // Wait long enough for logging to be output
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

            LOG.info("'{}' changed from '{}' to '{}'.",
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
