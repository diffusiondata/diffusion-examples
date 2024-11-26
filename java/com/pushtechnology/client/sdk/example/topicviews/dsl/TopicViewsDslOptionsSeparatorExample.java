/*******************************************************************************
 * Copyright (C) 2024 DiffusionData Ltd.
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pushtechnology.diffusion.client.Diffusion;
import com.pushtechnology.diffusion.client.callbacks.ErrorReason;
import com.pushtechnology.diffusion.client.features.Topics;
import com.pushtechnology.diffusion.client.session.Session;
import com.pushtechnology.diffusion.client.topics.details.TopicSpecification;
import com.pushtechnology.diffusion.client.topics.details.TopicType;
import com.pushtechnology.diffusion.datatype.json.JSON;

public class TopicViewsDslOptionsSeparatorExample {

    private static final Logger LOG =
        LoggerFactory.getLogger(TopicViewsDslOptionsSeparatorExample.class);

    public static void main(String[] args) {

        final Session session = Diffusion.sessions()
            .principal("admin")
            .password("password")
            .open("ws://localhost:8080");

        final Topics topics = session.feature(Topics.class);
        final TopicSpecification spec = Diffusion.newTopicSpecification(TopicType.JSON);
        final JSON value1 = Diffusion.dataTypes().json()
            .fromJsonString("{\"name\": \"Fred/Flintstone\"}");

        topics.addAndSet("my/topic/path/1", spec, JSON.class, value1).join();

        final JSON value2 = Diffusion.dataTypes().json()
            .fromJsonString("{\"name\": \"Wilma/Flintstone\"}");

        topics.addAndSet("my/topic/path/2", spec, JSON.class, value2).join();

        final JSON value3 = Diffusion.dataTypes().json()
            .fromJsonString("{\"name\": \"Wilma/Flintstone\"}");

        topics.addAndSet("my/topic/path/3", spec, JSON.class, value3).join();

        topics.addFallbackStream(JSON.class, new MyStream());
        topics.subscribe("?views//").join();

        topics.createTopicView(
            "topic_view_1",
            "map my/topic/path to views/<scalar(/name)> separator '_'").join();

        LOG.info("Topic View topic_view_1 has been created.");

        session.close();
    }

    public static class MyStream implements Topics.ValueStream<JSON> {

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
            JSON oldValue, JSON newValue) {
            System.out.printf("%s changed from %d to %d\n",
                topicPath, oldValue, newValue);
        }

        @Override
        public void onClose() {}

        @Override
        public void onError(ErrorReason errorReason) {}
    }
}
