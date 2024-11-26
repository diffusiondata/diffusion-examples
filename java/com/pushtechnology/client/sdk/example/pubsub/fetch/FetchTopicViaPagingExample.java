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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletionException;

public class FetchTopicViaPagingExample {

    private static final Logger LOG =
        LoggerFactory.getLogger(FetchTopicViaPagingExample.class);

    public static void main(String[] args)
        throws Throwable {

        try (Session session = Diffusion.sessions()
            .principal("admin")
            .password("password")
            .open("ws://localhost:8080")) {

            final Topics topics = session.feature(Topics.class);

            final TopicSpecification topicSpecification =
                Diffusion.newTopicSpecification(TopicType.STRING);

            for (int i = 1; i <= 25; i++) {
                final String topicPath = "my/topic/path/" + i;
                final String value = "diffusion data #" + i;

                topics.addAndSet(topicPath, topicSpecification, String.class, value)
                    .join();
            }

            final String topicSelectorString = "?my/topic/path//";

            Topics.FetchResult<String> fetchResult = topics.fetchRequest()
                .withValues(String.class)
                .first(10)
                .fetch(topicSelectorString)
                .join();

            while (true) {
                fetchResult.results().forEach(it -> LOG.info("{}: {}.", it.path(), it.value()));

                if (fetchResult.hasMore()) {
                    LOG.info("Loading next page.");

                    final Topics.FetchResult.TopicResult<String> topicResult =
                        fetchResult.results().get(fetchResult.size() - 1);

                    fetchResult = topics.fetchRequest()
                        .withValues(String.class)
                        .after(topicResult.path())
                        .first(10)
                        .fetch(topicSelectorString)
                        .join();
                }
                else {
                    LOG.info("Done.");

                    break;
                }
            }
        }
        catch (CompletionException e) {
            LOG.error("Failed to run example to completion.", e);

            throw e.getCause();
        }
    }
}
