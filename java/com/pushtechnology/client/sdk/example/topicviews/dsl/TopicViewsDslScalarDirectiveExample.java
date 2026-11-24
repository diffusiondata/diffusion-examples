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
 * This example demonstrates how to use the scalar directive in a topic view.
 * <P>
 * A JSON topic is created with account and balance information. The scalar directive
 * is used to dynamically create topic paths based on the account number and currency.
 *
 * @author DiffusionData Limited
 */
public class TopicViewsDslScalarDirectiveExample {

    private static final Logger LOG =
        LoggerFactory.getLogger(TopicViewsDslScalarDirectiveExample.class);

    public static void main(String[] args) throws Exception {

        final Session session = Diffusion.sessions()
            .principal("admin")
            .password("password")
            .open("ws://localhost:8080");

        final Topics topics = session.feature(Topics.class);
        final String viewSelector = "?views//";
        final Topics.ValueStream<JSON> valueStream = new MyStream();

        final JSON jsonValue = Diffusion.dataTypes().json()
            .fromJsonString("{\n" +
                "  \"account\": \"1234\"," +
                "  \"balance\": {" +
                "    \"amount\": 12.57," +
                "    \"currency\": \"USD\"" +
                "  }" +
                "}");

        topics.addAndSet("my/topic/path",
                Diffusion.newTopicSpecification(TopicType.JSON),
                JSON.class, jsonValue)
            .join();

        topics.addStream(viewSelector, JSON.class, valueStream);
        topics.subscribe(viewSelector).join();

        final TopicView myTopicView = topics.createTopicView("topic_view_1",
                " map my/topic/path to " +
                    "views/currency/<scalar(/balance/currency)>/account/<scalar(/account)>")
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
