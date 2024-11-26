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
package com.pushtechnology.client.sdk.example.pubsub.fetch;

import com.pushtechnology.diffusion.client.Diffusion;
import com.pushtechnology.diffusion.client.features.Topics;
import com.pushtechnology.diffusion.client.session.Session;
import com.pushtechnology.diffusion.client.topics.details.TopicSpecification;
import com.pushtechnology.diffusion.client.topics.details.TopicType;
import com.pushtechnology.diffusion.datatype.json.JSON;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.concurrent.CompletionException;

public class FetchTopicPropertiesExample {

    private static final Logger LOG =
        LoggerFactory.getLogger(FetchTopicPropertiesExample.class);

    public static void main(String[] args)
        throws Throwable {

        try (Session session = Diffusion.sessions()
            .principal("admin")
            .password("password")
            .open("ws://localhost:8080")) {

            final Topics topics = session.feature(Topics.class);

            final TopicSpecification specialisedJsonTopicSpecification = Diffusion.newTopicSpecification(TopicType.JSON)
                .withProperty(TopicSpecification.DONT_RETAIN_VALUE, Boolean.TRUE.toString())
                .withProperty(TopicSpecification.PERSISTENT, Boolean.FALSE.toString())
                .withProperty(TopicSpecification.PUBLISH_VALUES_ONLY, Boolean.TRUE.toString());

            for (int i = 1; i <= 5; i++) {
                final String topicPath = "my/topic/path/with/properties/" + i;

                final String jsonAsString = String.format("{ \"diffusion\": \"data #%d\" }", i);
                final JSON jsonValue = Diffusion.dataTypes().json().fromJsonString(jsonAsString);

                topics.addAndSet(topicPath, specialisedJsonTopicSpecification, JSON.class, jsonValue)
                    .join();
            }

            final TopicSpecification simpleStringTopicSpecification = Diffusion.newTopicSpecification(TopicType.STRING);

            for (int i = 1; i <= 5; i++) {
                final String topicPath = "my/topic/path/with/default/properties/" + i;
                final String value = "diffusion data #" + i;

                topics.addAndSet(topicPath, simpleStringTopicSpecification, String.class, value)
                    .join();
            }

            final String topicSelectorString = "?my/topic/path//";

            final Topics.FetchResult<Void> jsonTopicsFetchResult = topics.fetchRequest()
                .topicTypes(Collections.singleton(TopicType.JSON))
                .withProperties()
                .fetch(topicSelectorString)
                .join();

            jsonTopicsFetchResult.results().forEach(topicResult -> {
                    LOG.info("{} properties:", topicResult.path());

                    topicResult.specification().getProperties().forEach((key, value) -> {
                            LOG.info("* {}: {}.", key, value);
                        }
                    );
                }
            );

            final Topics.FetchResult<Void> stringFetchResult = topics.fetchRequest()
                .topicTypes(Collections.singleton(TopicType.STRING))
                .withProperties()
                .fetch(topicSelectorString)
                .join();

            stringFetchResult.results().forEach(topicResult -> {
                    LOG.info("{} properties:", topicResult.path());

                    topicResult.specification().getProperties().forEach((key, value) -> {
                            LOG.info("* {}: {}.", key, value);
                        }
                    );
                }
            );
        }
        catch (CompletionException e) {
            LOG.error("Failed to run example to completion.", e);

            throw e.getCause();
        }
    }
}
