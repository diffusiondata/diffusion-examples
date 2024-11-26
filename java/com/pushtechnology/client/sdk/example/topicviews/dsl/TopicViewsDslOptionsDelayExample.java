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
import com.pushtechnology.diffusion.client.features.control.topics.TopicControl;
import com.pushtechnology.diffusion.client.session.Session;
import com.pushtechnology.diffusion.client.topics.details.TopicSpecification;
import com.pushtechnology.diffusion.client.topics.details.TopicType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TopicViewsDslOptionsDelayExample {

    private static final Logger LOG =
        LoggerFactory.getLogger(TopicViewsDslOptionsDelayExample.class);

    public static void main(String[] args) throws Exception {

        Session session = Diffusion.sessions()
            .principal("admin")
            .password("password")
            .open("ws://localhost:8080");

        final Topics topics = session.feature(Topics.class);
        final String topicPath = "my/topic/path";

        topics.addAndSet(
                topicPath,
                Diffusion.newTopicSpecification(TopicType.INT64), Long.class, 0L)
            .join();

        topics.addFallbackStream(Long.class, new MyFallbackStream());
        topics.subscribe("?views//").join();

        final String topicViewName = "topic_view_1";

        topics.createTopicView(topicViewName,
                "map my/topic/path to views/<path(0)> delay by 5 seconds")
            .join();

        System.out.println(topicViewName + " has been created");

        for (int i = 0; i < 15; i++) {
            topics.set(topicPath, Long.class, System.currentTimeMillis()).join();
            SECONDS.sleep(1);
        }

        topics.removeTopicView(topicViewName).join();
        session.feature(TopicControl.class).removeTopics("?.*//").join();
        session.close();

        LOG.info("Topic View <topic_view_1> has been created");
    }
    public static class MyFallbackStream implements Topics.ValueStream<Long> {

        @Override
        public void onSubscription(String topicPath,
            TopicSpecification topicSpecification) {
            System.out.printf("Subscribed to %s\n", topicPath);
        }

        @Override
        public void onUnsubscription(String topicPath,
            TopicSpecification topicSpecification,
            Topics.UnsubscribeReason unsubscribeReason) {
            System.out.printf("Unsubscribed from %s: %s\n", topicPath, unsubscribeReason);
        }

        @Override
        public void onValue(String topicPath,
            TopicSpecification topicSpecification,
            Long oldValue, Long newValue) {
            System.out.printf("%s changed from %d to %d\n",
                topicPath, oldValue, newValue);
        }

        @Override
        public void onClose() {}

        @Override
        public void onError(ErrorReason errorReason) {}
    }
}
