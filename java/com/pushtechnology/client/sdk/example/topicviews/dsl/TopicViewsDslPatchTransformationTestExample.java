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
 * This example demonstrates how to use the patch transformation to apply a test operation
 * within a JSON topic.
 * <P>
 * A topic view is created that verifies whether a specific key's value matches an expected value
 * before allowing transformations to proceed.
 *
 * @author DiffusionData Limited
 */
public class TopicViewsDslPatchTransformationTestExample {

    private static final Logger LOG =
        LoggerFactory.getLogger(TopicViewsDslPatchTransformationTestExample.class);

    public static void main(String[] args) throws Exception {

        final Session session = Diffusion.sessions()
            .principal("admin")
            .password("password")
            .open("ws://localhost:8080");

        final Topics topics = session.feature(Topics.class);
        final String viewSelector = "?views//";
        final Topics.ValueStream<JSON> valueStream = new MyStream();

        final JSON jsonValue = Diffusion.dataTypes().json()
            .fromJsonString("{\"Fred\":\"Flintstone\",\"Barney\":\"Rubble\",\"George\":\"Jetson\"}");

        topics.addAndSet("my/topic/path",
                Diffusion.newTopicSpecification(TopicType.JSON),
                JSON.class, jsonValue)
            .join();

        topics.addStream(viewSelector, JSON.class, valueStream);
        topics.subscribe(viewSelector).join();

        final TopicView view = topics.createTopicView("topic_view_1",
                "map ?my/topic/path// to views/<path(0)> patch " +
                    "'[{\"op\": \"test\", \"path\": \"/Fred\", \"value\": \"Flintstone\" }]'")
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
