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

import static java.util.concurrent.TimeUnit.MILLISECONDS;

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
 * This example demonstrates how to use the topic view throttle clause.
 * <P>
 * A topic view is created that limits updates to at most one every 10 seconds,
 * even if the source topic updates more frequently.
 *
 * @author DiffusionData Limited
 */
public class TopicViewsDslOptionsThrottleExample {

    private static final Logger LOG =
        LoggerFactory.getLogger(TopicViewsDslOptionsThrottleExample.class);

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

        final TopicView myView = topics.createTopicView("topic_view_1",
                "map my/topic/path to views/<path(0)> throttle to 1 update every 10 seconds")
            .join();

        LOG.info("Topic View {} has been created", myView.getName());

        for (int i = 0; i < 5; i++) {
            topics.set(topicPath, Long.class, System.currentTimeMillis()).join();
            MILLISECONDS.sleep(500);
        }

        topics.removeStream(valueStream);
        session.close();
    }

    private static final class MyStream implements Topics.ValueStream<Long> {

        @Override
        public void onValue(
            String topicPath,
            TopicSpecification topicSpecification,
            Long oldValue,
            Long newValue) {
            LOG.info("{} new value {}", topicPath, newValue);
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
