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

/**
 * This example demonstrates how to use the topic view delay clause.
 * <P>
 * A topic view with a 5-second delay is created.
 *
 * @author DiffusionData Limited
 */
public class TopicViewsDslOptionsDelayExample {

    private static final Logger LOG =
        LoggerFactory.getLogger(TopicViewsDslOptionsDelayExample.class);

    public static void main(String[] args) throws Exception {

        final Session session = Diffusion.sessions()
            .principal("admin")
            .password("password")
            .open("ws://localhost:8080");

        final Topics topics = session.feature(Topics.class);
        final String topicPath = "my/topic/path";
        final String viewSelector = "?views//";
        final Topics.ValueStream<Long> valueStream = new MyStream();

        topics.addAndSet(
                topicPath,
                Diffusion.newTopicSpecification(TopicType.INT64), Long.class, 0L)
            .join();

        topics.addStream(viewSelector, Long.class, valueStream);
        topics.subscribe(viewSelector).join();

        final TopicView topicView = topics.createTopicView("topic_view_1",
                "map my/topic/path to views/<path(0)> delay by 5 seconds")
            .join();

        LOG.info("Topic View {} has been created", topicView.getName());

        for (int i = 0; i < 15; i++) {
            topics.set(topicPath, Long.class, System.currentTimeMillis()).join();
            SECONDS.sleep(1);
        }

        topics.removeStream(valueStream);
        session.close();
    }

    public static class MyStream implements Topics.ValueStream<Long> {

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
        public void onValue(String topicPath,
            TopicSpecification topicSpecification,
            Long oldValue,
            Long newValue) {
            LOG.info("{} new value {}", topicPath, newValue);
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
