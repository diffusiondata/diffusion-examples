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
package com.pushtechnology.client.sdk.example.pubsub.jsonpatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pushtechnology.diffusion.client.Diffusion;
import com.pushtechnology.diffusion.client.callbacks.ErrorReason;
import com.pushtechnology.diffusion.client.features.TopicUpdate;
import com.pushtechnology.diffusion.client.features.Topics;
import com.pushtechnology.diffusion.client.session.Session;
import com.pushtechnology.diffusion.client.topics.details.TopicSpecification;
import com.pushtechnology.diffusion.client.topics.details.TopicType;
import com.pushtechnology.diffusion.datatype.json.JSON;

public class JSONPatchCopyExample {

    private static final Logger LOG =
        LoggerFactory.getLogger(JSONPatchCopyExample.class);

    public static void main(String[] args) {

        try (Session session = Diffusion.sessions()
            .principal("admin")
            .password("password")
            .open("ws://localhost:8080")) {

            final Topics topics = session.feature(Topics.class);
            final TopicUpdate topicUpdate = session.feature(TopicUpdate.class);
            final TopicSpecification spec = Diffusion.newTopicSpecification(TopicType.JSON);
            final JSON jsonValue = Diffusion.dataTypes().json()
                .fromJsonString(
                    "{\"Meet the Flintstones\": {" +
                        "\"Fred\": \"Flintstone\"," +
                        "\"Barney\": \"Rubble\"}," +
                    "\"The Jetsons\": {" +
                        "\"George\" : \"Jetson\"}}");

            topicUpdate.addAndSet(
                "my/topic/path",
                spec,
                JSON.class,
                jsonValue
            ).join();

            final Topics.ValueStream<JSON> myStream = new MyStream(LOG);
            topics.addFallbackStream(JSON.class, myStream);
            topics.subscribe("my/topic/path").join();

            topicUpdate.applyJsonPatch(
                "my/topic/path",
                "[{\"op\":\"copy\", \"from\": \"/Meet the Flintstones/Fred\", \"path\": \"/The Jetsons/George\"}]"
            ).join();

            topics.removeStream(myStream);
        }
    }

    static class MyStream implements Topics.ValueStream<JSON> {

        private Logger LOG;

        public MyStream(Logger logger) {
            this.LOG = logger;
        }
        @Override
        public void onValue(
            String topicPath,
            TopicSpecification topicSpecification,
            JSON oldValue,
            JSON newValue) {
            LOG.info("{} new value {}",
                topicPath, newValue);
        }

        @Override
        public void onSubscription(String topicPath, TopicSpecification topicSpecification) {
            LOG.info("Subscribed to: {}", topicPath);
        }

        @Override
        public void onUnsubscription(String topicPath, TopicSpecification topicSpecification,
            Topics.UnsubscribeReason unsubscribeReason) {
            LOG.info("Unsubscribed from: {}, reason: {}",
                topicPath, unsubscribeReason);
        }

        @Override
        public void onClose() {
            LOG.info("On close");
        }

        @Override
        public void onError(ErrorReason errorReason) {
            LOG.error("On error: {}", errorReason);
        }
    }
}
