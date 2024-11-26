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

public class TopicViewsDslOptionsTopicPropertyMappingExample {

    private static final Logger LOG =
        LoggerFactory.getLogger(TopicViewsDslOptionsTopicPropertyMappingExample.class);

    public static void main(String[] args) {
        Session session = Diffusion.sessions()
            .principal("admin")
            .password("password")
            .open("ws://localhost:8080");

        final Topics topics = session.feature(Topics.class);
        final JSON jsonValue = Diffusion.dataTypes().json()
            .fromJsonString("[" +
                "\"Fred Flintstone\", " +
                "\"Wilma Flintstone\", " +
                "\"Barney Rubble\", " +
                "\"Betty Rubble\"" +
                "]");

        topics.addAndSet(
                "my/topic/path",
                Diffusion.newTopicSpecification(TopicType.JSON), JSON.class, jsonValue)
            .join();

        topics.addFallbackStream(JSON.class, new MyFallbackStream());
        topics.subscribe("?views//").join();

        final String topicViewName = "topic_view_1";

        topics.createTopicView(topicViewName,
                "map my/topic/path to views/<path(0)> with properties " +
                    "'CONFLATION':'off', 'COMPRESSION':'false', 'DONT_RETAIN_VALUE':'true'")
            .join();

        System.out.println(topicViewName + " has been created");

        topics.removeTopicView(topicViewName).join();
        session.feature(TopicControl.class).removeTopics("?.*//").join();
        session.close();

        LOG.info("Topic View <topic_view_1> has been created");
    }

    public static class MyFallbackStream implements Topics.ValueStream<JSON> {

        @Override
        public void onSubscription(String topicPath,
            TopicSpecification topicSpecification) {
            System.out.printf("Subscribed to %s\n", topicPath);
        }

        @Override
        public void onValue(
            String topicPath,
            TopicSpecification topicSpecification, JSON oldValue, JSON newValue) {}

        @Override
        public void onUnsubscription(
            String topicPath,
            TopicSpecification topicSpecification,
            Topics.UnsubscribeReason unsubscribeReason) {}

        @Override
        public void onClose() {}

        @Override
        public void onError(ErrorReason errorReason) {}
    }
}
