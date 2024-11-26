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
package com.pushtechnology.client.sdk.example.pubsub.remove;

import static java.lang.String.format;

import com.pushtechnology.diffusion.client.Diffusion;
import com.pushtechnology.diffusion.client.features.control.topics.TopicControl;
import com.pushtechnology.diffusion.client.session.Session;
import com.pushtechnology.diffusion.client.topics.details.TopicSpecification;
import com.pushtechnology.diffusion.client.topics.details.TopicType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletionException;

public class RemoveMultipleTopicsViaSelectorExample {

    private static final Logger LOG =
        LoggerFactory.getLogger(RemoveMultipleTopicsViaSelectorExample.class);

    public static void main(String[] args)
        throws Throwable {

        try (Session session = Diffusion.sessions()
            .principal("admin")
            .password("password")
            .open("ws://localhost:8080")) {

            final TopicControl topicControl = session.feature(TopicControl.class);

            final TopicSpecification topicSpecification = Diffusion.newTopicSpecification(TopicType.JSON);

            topicControl.addTopic("this/topic1", topicSpecification)
                .join();

            topicControl.addTopic("this/topic2", topicSpecification)
                .join();

            final TopicControl.AddTopicResult addResult1 =
                topicControl.addTopic(
                        "my/topic/path/to/be/removed", topicSpecification)
                    .join();

            if (addResult1 == TopicControl.AddTopicResult.CREATED) {
                LOG.info("Topic has been created.");
            }
            else {
                LOG.info("Topic already exists.");
            }

            final TopicControl.AddTopicResult addResult2 =
                topicControl.addTopic(
                        "my/topic/path/to/be/also/removed", topicSpecification)
                    .join();

            if (addResult2 == TopicControl.AddTopicResult.CREATED) {
                LOG.info("Second topic has been created.");
            }
            else {
                LOG.info("Second topic already exists.");
            }

            final TopicControl.TopicRemovalResult removeResult =
                topicControl.removeTopics("?my/topic/path/to/be//")
                    .join();

            final int removedCount = removeResult.getRemovedCount();

            if (removedCount == 2) {
                LOG.info("Topics have been removed.");
            }
            else {
                final String message = format(
                    "Topics expected to be removed, but %d topics were removed.",
                    removedCount);

                LOG.error(message);
                throw new IllegalStateException(message);
            }
        }
        catch (CompletionException e) {
            LOG.error("Failed to add or remove topic.", e);

            throw e.getCause();
        }
    }
}
