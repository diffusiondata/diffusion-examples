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
import com.pushtechnology.diffusion.client.features.control.topics.views.TopicView;
import com.pushtechnology.diffusion.client.session.Session;
import com.pushtechnology.diffusion.client.topics.details.TopicSpecification;
import com.pushtechnology.diffusion.client.topics.details.TopicType;
import com.pushtechnology.diffusion.datatype.json.JSON;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This example demonstrates how to use the process transformation to set new fields
 * in a JSON topic within a topic view.
 * <P>
 * A topic view is created that assigns a Tier value based on the balance amount,
 * and calculates the amount in cents as a new field.
 *
 * @author DiffusionData Limited
 */
public class TopicViewsDslProcessTransformationSetExample {

    private static final Logger LOG =
        LoggerFactory.getLogger(TopicViewsDslProcessTransformationSetExample.class);

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
                "  \"account\": \"1234\"," +
                "  \"balance\": {" +
                "    \"amount\": 12.57," +
                "    \"currency\": \"USD\"" +
                "  }" +
                "}");

        topics.addAndSet("my/topic/path/1", mySpec, JSON.class, jsonValue1).join();

        final JSON jsonValue2 = Diffusion.dataTypes().json()
            .fromJsonString("{\n" +
                "  \"account\": \"5678\"," +
                "  \"balance\": {" +
                "    \"amount\": 98.76," +
                "    \"currency\": \"USD\"" +
                "  }" +
                "}");

        topics.addAndSet("my/topic/path/2", mySpec, JSON.class, jsonValue2).join();
        topics.addStream(viewSelector, JSON.class, valueStream);
        topics.subscribe(viewSelector).join();

        final TopicView myTopicView = topics.createTopicView("topic_view_1",
                "map ?my/topic/path// to views/<path(3)> process {\n" +
                    "  if '/balance/amount > 20'\n" +
                    "    set(/Tier, 1)\n" +
                    "  else\n" +
                    "    set(/Tier, 2)\n" +
                    "}\n" +
                    "process {\n" +
                    "  set(/balance/amount_in_cents, calc '/balance/amount * " +
                    "100')\n" +
                    "}")
            .join();

        LOG.info("Topic View {} has been created", myTopicView.getName());

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
