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
package com.pushtechnology.client.sdk.example.monitoring;

import static java.util.concurrent.TimeUnit.SECONDS;

import com.pushtechnology.diffusion.client.Diffusion;
import com.pushtechnology.diffusion.client.callbacks.ErrorReason;
import com.pushtechnology.diffusion.client.callbacks.Registration;
import com.pushtechnology.diffusion.client.features.Topics;
import com.pushtechnology.diffusion.client.features.control.topics.TopicControl;
import com.pushtechnology.diffusion.client.session.Session;
import com.pushtechnology.diffusion.client.topics.details.TopicSpecification;
import com.pushtechnology.diffusion.client.topics.details.TopicType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MonitoringMissingTopicNotificationsExample {

    private static final Logger LOG =
        LoggerFactory.getLogger(MonitoringMissingTopicNotificationsExample.class);

    public static void main(String[] args)
        throws Exception {

        final String serverUrl = "ws://localhost:8080";

        try (Session session1 = Diffusion.sessions()
            .principal("admin")
            .password("password")
            .open(serverUrl)) {

            final String topicPath = "my/topic/path";

            final Registration registration =
                session1.feature(TopicControl.class).addMissingTopicHandler(topicPath,
                        new MyMissingTopicStream(session1))
                    .join();

            final Session session2 = Diffusion.sessions()
                .principal("client").password("password").open(serverUrl);

            final Topics session2Topics = session2.feature(Topics.class);

            final MyLoggingStringStream myLoggingStringStream = new MyLoggingStringStream();

            session2Topics.addStream("?my/topic/path", String.class, myLoggingStringStream);
            session2Topics.subscribe("my/topic/path/does/not/exist/yet")
                .join();

            LOG.info("Missing topic will have been created.");

            session2Topics.removeStream(myLoggingStringStream);

            SECONDS.sleep(2);

            registration.close()
                .join();

            session2.close();
        }
    }

    public static class MyMissingTopicStream implements TopicControl.MissingTopicNotificationStream {
        private static final Logger LOG = LoggerFactory.getLogger(MyMissingTopicStream.class);

        private final Session session;

        public MyMissingTopicStream(Session session) {
            this.session = session;
        }

        @Override
        public void onMissingTopic(TopicControl.MissingTopicNotification missingTopicNotification) {
            final String topicPath = missingTopicNotification.getTopicPath();

            LOG.info("On missing topic: '{}'.", topicPath);

            session.feature(TopicControl.class).addTopic(topicPath, TopicType.STRING)
                .whenComplete((addTopicResult, throwable) -> {
                    if (throwable == null) {
                        LOG.info("Topic created: '{}'.", topicPath);
                    }
                });
        }

        @Override
        public void onClose() {
            LOG.info("On close.");
        }

        @Override
        public void onError(ErrorReason errorReason) {
            LOG.info("On error: {}.", errorReason);
        }
    }

    public static class MyLoggingStringStream implements Topics.ValueStream<String> {
        private static final Logger LOG = LoggerFactory.getLogger(MyLoggingStringStream.class);

        @Override
        public void onValue(
            String topicPath,
            TopicSpecification topicSpecification,
            String oldValue,
            String newValue) {

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
