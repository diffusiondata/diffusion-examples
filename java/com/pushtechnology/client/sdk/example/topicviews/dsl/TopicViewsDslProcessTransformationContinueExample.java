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
 * This example demonstrates how to use the process transformation with a continue condition
 * in a topic view.
 * <P>
 * A topic view is created that filters topics based on the value of the `/price_per_share` field,
 * only allowing topics where the price is greater than 20 to be included in the view.
 *
 * @author DiffusionData Limited
 */
public class TopicViewsDslProcessTransformationContinueExample {

    private static final Logger LOG =
        LoggerFactory.getLogger(TopicViewsDslProcessTransformationContinueExample.class);

    public static void main(String[] args) throws Exception {

        final Session session = Diffusion.sessions()
            .principal("admin")
            .password("password")
            .open("ws://localhost:8080");

        final Topics topics = session.feature(Topics.class);
        final String viewSelector = "?views//";
        final Topics.ValueStream<JSON> valueStream = new MyStream();

        final TopicSpecification mySpec = Diffusion
            .newTopicSpecification(TopicType.JSON);

        final JSON jsonValue1 = Diffusion.dataTypes().json()
            .fromJsonString("{\n" +
                "  \"name\": \"APPL\",\n" +
                "  \"quantity\": 100,\n" +
                "  \"price_per_share\": 12.34\n" +
                "}");

        topics.addAndSet("my/topic/path/1", mySpec, JSON.class, jsonValue1).join();

        final JSON jsonValue2 = Diffusion.dataTypes().json()
            .fromJsonString("{\n" +
                "  \"name\": \"AMZN\",\n" +
                "  \"quantity\": 256,\n" +
                "  \"price_per_share\": 87.65\n" +
                "}");

        topics.addAndSet("my/topic/path/2", mySpec, JSON.class, jsonValue2).join();

        topics.addStream(viewSelector, JSON.class, valueStream);
        topics.subscribe(viewSelector).join();

        final TopicView view = topics.createTopicView("topic_view_1",
                "map ?my/topic/path// to views/<path(3)> process\n" +
                    "{\n" +
                    "  if '/price_per_share > 20' continue\n" +
                    "}")
            .join();

        LOG.info("Topic View {} has been created", view.getName());

        SECONDS.sleep(1);

        topics.removeStream(valueStream);
        session.close();
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
