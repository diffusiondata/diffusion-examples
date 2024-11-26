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
import com.pushtechnology.diffusion.client.features.control.topics.views.TopicView;
import com.pushtechnology.diffusion.client.session.Session;
import com.pushtechnology.diffusion.client.topics.details.TopicSpecification;
import com.pushtechnology.diffusion.client.topics.details.TopicType;
import com.pushtechnology.diffusion.datatype.json.JSON;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TopicViewsDslOptionsPreserveTopicsExample {

    private static final Logger LOG =
        LoggerFactory.getLogger(TopicViewsDslOptionsPreserveTopicsExample.class);

    public static void main(String[] args) throws Exception {

        Session session = Diffusion.sessions()
            .principal("admin")
            .password("password")
            .open("ws://localhost:8080");

        final Topics topics = session.feature(Topics.class);
        final TopicSpecification specification = Diffusion.newTopicSpecification(TopicType.JSON);
        final String topicPath = "my/topic/path";

        final JSON value1 = Diffusion.dataTypes().json()
            .fromJsonString("{\"name\":\"Fred Flintstone\"}");

        final JSON value2 = Diffusion.dataTypes().json()
            .fromJsonString("{\"name\":\"Wilma Flintstone\"}");

        final JSON value3 = Diffusion.dataTypes().json()
            .fromJsonString("{\"name\":\"Pebbles Flintstone\"}");

        topics.addAndSet(topicPath, specification, JSON.class, value1)
            .join();

        topics.addFallbackStream(JSON.class, new MyFallbackStream());
        topics.subscribe("?views//").join();

        final TopicView view1 = topics.createTopicView("topic_view_1",
                "map my/topic/path to views/preserved/<scalar(/name)> preserve topics").join();

        LOG.info("Topic view created: {}", view1.getName());

        final TopicView view2 = topics.createTopicView("topic_view_2",
                "map my/topic/path to views/not_preserved/<scalar(/name)>").join();

        LOG.info("Topic view created: {}", view2.getName());

        topics.set(topicPath, JSON.class, value2).join();
        topics.set(topicPath, JSON.class, value3).join();

        topics.removeTopicView("topic_view_1").join();
        topics.removeTopicView("topic_view_2").join();

        session.feature(TopicControl.class).removeTopics("?.*//").join();
        session.close();
    }

    public static class MyFallbackStream implements Topics.ValueStream<JSON> {

        @Override
        public void onSubscription(String topicPath,
            TopicSpecification topicSpecification) {
            LOG.info("Subscribed to {}", topicPath);
        }

        @Override
        public void onValue(
            String topicPath,
            TopicSpecification topicSpecification, JSON oldValue, JSON newValue) {
            LOG.info("New value for {} : {}", topicPath, newValue);
        }

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
