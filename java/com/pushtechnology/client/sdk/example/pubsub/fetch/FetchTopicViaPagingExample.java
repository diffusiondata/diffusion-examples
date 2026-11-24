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

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pushtechnology.diffusion.client.Diffusion;
import com.pushtechnology.diffusion.client.features.Topics;
import com.pushtechnology.diffusion.client.features.Topics.FetchResult.TopicResult;
import com.pushtechnology.diffusion.client.session.Session;
import com.pushtechnology.diffusion.client.topics.details.TopicSpecification;
import com.pushtechnology.diffusion.client.topics.details.TopicType;

/**
 * This example demonstrates how to fetch topics in Diffusion using paging.
 * <P>
 * The example creates a set of STRING topics, then uses the fetch request feature
 * with paging to retrieve the topics in batches, allowing efficient handling of
 * large topic trees.
 *
 * @author DiffusionData Limited
 */
public class FetchTopicViaPagingExample {

    private static final Logger LOG =
        LoggerFactory.getLogger(FetchTopicViaPagingExample.class);

    public static void main(String[] args) {

        try (Session session = Diffusion.sessions()
            .principal("admin")
            .password("password")
            .open("ws://localhost:8080")) {

            final Topics topics = session.feature(Topics.class);
            final String topicSelectorString = "?my/topic/path//";

            final TopicSpecification topicSpecification =
                Diffusion.newTopicSpecification(TopicType.STRING);

            for (int i = 1; i <= 25; i++) {
                final String topicPath = "my/topic/path/" + i;
                final String value = "diffusion data #" + i;

                topics.addAndSet(topicPath, topicSpecification, String.class, value)
                    .join();
            }

            final List<TopicResult<String>> topicResults = new ArrayList<>();

            final Topics.FetchRequest<String> request = topics.fetchRequest()
                .withValues(String.class)
                .first(10);

            Topics.FetchResult<String> pagedFetch = request
                .fetch(topicSelectorString)
                .join();

            topicResults.addAll(pagedFetch.results());

            while (pagedFetch.hasMore()) {

                LOG.info("Loading next page.");

                final String lastTopic = topicResults.get(topicResults.size() - 1).path();

                pagedFetch = request.after(lastTopic).fetch(topicSelectorString).join();

                topicResults.addAll(pagedFetch.results());
            }

            topicResults.forEach(topicResult ->
                LOG.info("{}: {}.", topicResult.path(), topicResult.value()));
        }
    }
}
