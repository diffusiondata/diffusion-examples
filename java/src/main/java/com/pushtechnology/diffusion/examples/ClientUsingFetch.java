/*******************************************************************************
 * Copyright (C) 2018 Push Technology Ltd.
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
package com.pushtechnology.diffusion.examples;

import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pushtechnology.diffusion.client.Diffusion;
import com.pushtechnology.diffusion.client.features.Topics;
import com.pushtechnology.diffusion.client.features.Topics.FetchResult;
import com.pushtechnology.diffusion.client.features.Topics.FetchResult.TopicResult;
import com.pushtechnology.diffusion.client.session.Session;
import com.pushtechnology.diffusion.client.topics.TopicSelector;
import com.pushtechnology.diffusion.client.topics.details.TopicType;
import com.pushtechnology.diffusion.datatype.Bytes;
import com.pushtechnology.diffusion.datatype.json.JSON;

/**
 * This provides various examples of using the new fetch capabilities introduced
 * in release 6.2.
 * <P>
 * This makes use of the 'Topics' feature only.
 *
 * @author Push Technology Limited
 * @since 6.2
 */
public final class ClientUsingFetch {

    private static final Logger LOG =
        LoggerFactory.getLogger(ClientUsingFetch.class);

    private final Session session;
    private final Topics topics;

    /**
     * Constructor.
     */
    public ClientUsingFetch(String serverUrl) {

        session =
            Diffusion.sessions().principal("client").password("password")
                .open(serverUrl);

        topics = session.feature(Topics.class);
    }

    /**
     * This shows an example of retrieving all topics - only topic path and type
     * are returned in each result.
     */
    public List<TopicResult<Void>> fetchAll()
        throws InterruptedException, ExecutionException, TimeoutException {

        return topics.fetchRequest().fetch("*.*").get(5, SECONDS).results();
    }

    /**
     * This shows an example of retrieving all string topics that satisfy a
     * specified topic selector with values.
     */
    public List<TopicResult<String>>
        fetchAllStringTopics(TopicSelector selector)
            throws InterruptedException, ExecutionException, TimeoutException {

        return topics.fetchRequest()
            .withValues(String.class)
            .fetch(selector).get(5, SECONDS).results();
    }

    /**
     * This shows an example of retrieving a single string topic with value and
     * properties.
     */
    public TopicResult<String> fetchStringTopic(String topicPath)
        throws InterruptedException, ExecutionException, TimeoutException {
        return
            topics.fetchRequest()
                .withValues(String.class)
                .withProperties()
                .fetch(topicPath)
                .get(5, SECONDS)
                .results()
                .stream()
                .findFirst()
                .orElse(null);
    }

    /**
     * This shows how to obtain the value of a specified string topic.
     * <p>
     * Uses {@link #fetchStringTopic(String)}.
     */
    public String getStringTopicValue(String topicPath)
        throws InterruptedException, ExecutionException, TimeoutException {

        final TopicResult<String> result = fetchStringTopic(topicPath);
        return result != null ? result.value() : null;
    }

    /**
     * This shows an example of retrieving all JSON topics that match a
     * specified selector with values.
     *
     * This would return results only for JSON topics and not type compatible
     * subtypes.
     */
    public List<TopicResult<JSON>> fetchJSONTopics(TopicSelector selector)
        throws InterruptedException, ExecutionException, TimeoutException {

        return topics.fetchRequest()
            .withValues(JSON.class)
            .topicTypes(EnumSet.of(TopicType.JSON))
            .fetch(selector).get(5, SECONDS).results();
    }

    /**
     * Shows how to obtain an inclusive range of topics, with values.
     */
    public List<TopicResult<Bytes>> fetchRange(String from, String to)
        throws InterruptedException, ExecutionException, TimeoutException {

        return topics.fetchRequest()
            .from(from)
            .to(to)
            .withValues(Bytes.class)
            .fetch("*.*").get(5, SECONDS).results();
    }

    /**
     * Shows how to obtain the next group of topics, with values, from a
     * specified start point.
     * <p>
     * This demonstrates paging and could be used repeatedly specifying the
     * after value as the path of the last topic retrieved from the previous
     * call of the next method. The {@link FetchResult#hasMore() hasMore} method
     * on the result can be used to determine whether there may be more results.
     * <p>
     * Bytes is used as the value type so that all topic types are selected.
     */
    public FetchResult<Bytes> next(String after, int limit)
        throws InterruptedException, ExecutionException, TimeoutException {

        return topics.fetchRequest()
            .after(after)
            .withValues(Bytes.class)
            .first(limit)
            .fetch("*.*").get(5, SECONDS);
    }

    /**
     * Shows how to obtain the prior group of topics, with values, from a
     * specified end point.
     * <p>
     * This demonstrates paging and could be used to retrieve the set of topics
     * prior to the first topic from a previous call of prior or next.
     */
    public FetchResult<Bytes> prior(String before, int limit)
        throws InterruptedException, ExecutionException, TimeoutException {

        return topics.fetchRequest()
            .before(before)
            .withValues(Bytes.class)
            .last(limit)
            .fetch("*.*").get(5, SECONDS);
    }

    /**
     * Shows how to utilise deep branching limits, with values, from
     * a start point and limiting the number of matching topics.
     * <p>
     * This demonstrates how one could easily limit the number of results of similar
     * topics, or children of topics.
     */
    public FetchResult<Bytes> limitDeepBranches(String start, int limitTopLevel, int limitPerBranch)
        throws InterruptedException, ExecutionException, TimeoutException {
            return topics.fetchRequest()
            .withValues(Bytes.class)
            .limitDeepBranches(limitTopLevel, limitPerBranch)
            .fetch(start).get(5, SECONDS);
        }

    /**
     * This example shows how to log the values of all STRING topics, grouped
     * into pages of size as specified.
     */
    public void listAllStringTopics(int pageSize)
        throws InterruptedException, ExecutionException, TimeoutException {

        int page = 1;

        // Fetch the first page
        FetchResult<String> result =
            topics.fetchRequest()
                .withValues(String.class)
                .first(pageSize)
                .fetch("*.*").get(5, SECONDS);

        // Log the page results repeatedly until no more
        while (logPage(page, result)) {
            result =
                topics.fetchRequest()
                    .after(result.results().get(pageSize - 1).path())
                    .withValues(String.class)
                    .first(pageSize)
                    .fetch("*.*").get(5, SECONDS);
            page++;
        }
    }

    private static boolean logPage(int pageNumber, FetchResult<String> result) {
        final List<TopicResult<String>> results = result.results();
        if (results.size() > 0) {
            LOG.info("Page {}", pageNumber);
            result.results().forEach(r -> {
                LOG.info("{} = {}", r.path(), r.value());
            });
        }
        return result.hasMore();
    }

    /**
     * Close the session.
     */
    public void close() {
        session.close();
    }

}
