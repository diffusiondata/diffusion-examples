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

import com.pushtechnology.diffusion.client.Diffusion;
import com.pushtechnology.diffusion.client.callbacks.ErrorReason;
import com.pushtechnology.diffusion.client.features.TopicUpdate;
import com.pushtechnology.diffusion.client.features.control.topics.TopicNotifications;
import com.pushtechnology.diffusion.client.features.control.topics.TopicNotifications.TopicNotificationListener;
import com.pushtechnology.diffusion.client.session.Session;
import com.pushtechnology.diffusion.client.topics.details.TopicSpecification;
import com.pushtechnology.diffusion.client.topics.details.TopicType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class MonitoringTopicNotificationsExample {

    public static void main(String[] args) {

        Session session = Diffusion.sessions()
            .principal("admin")
            .password("password")
            .open("ws://localhost:8080");

        final TopicUpdate topicUpdate = session.feature(TopicUpdate.class);
        final TopicNotifications notifications = session.feature(TopicNotifications.class);
        final TopicSpecification myTopicSpec = Diffusion.newTopicSpecification(
            TopicType.STRING);

        TopicNotifications.NotificationRegistration registration =
            notifications.addListener(new MyTopicListener()).join();

        registration.select(">my");

        Map<String, String> myTopicData = new HashMap<String, String>() {{
            put("my/topic/path", "Good morning");
            put("my/other/topic/path", "Good afternoon");
            put("other/path/of/the/topic/tree", "This will not generate a notification");
        }};

        myTopicData.forEach((path, value) ->
            topicUpdate.addAndSet(
                path,
                myTopicSpec,
                String.class,
                value)
            .join());

        registration.close().join();
        session.close();
    }

    static class MyTopicListener implements TopicNotificationListener {

        static final Logger LOG =
            LoggerFactory.getLogger(MyTopicListener.class);

        @Override
        public void onTopicNotification(String topicPath,
            TopicSpecification topicSpecification,
            NotificationType notificationType) {

            LOG.info("Topic {} has been {}",
                topicPath, notificationType.name().toLowerCase());
        }

        @Override
        public void onDescendantNotification(String topicPath,
            NotificationType notificationType) {

            LOG.info("Descendant Topic {} has been {}",
                topicPath, notificationType.name().toLowerCase());

        }

        @Override
        public void onClose() {
            LOG.info("closed");
        }

        @Override
        public void onError(ErrorReason errorReason) {
            LOG.error("Error: {}", errorReason);
        }
    }
}
