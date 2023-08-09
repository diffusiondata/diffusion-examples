/*******************************************************************************
 * Copyright (c) 2019 Push Technology Ltd., All Rights Reserved.
 *
 * Use is subject to license terms.
 *
 * NOTICE: All information contained herein is, and remains the
 * property of Push Technology. The intellectual and technical
 * concepts contained herein are proprietary to Push Technology and
 * may be covered by U.S. and Foreign Patents, patents in process, and
 * are protected by trade secret or copyright law.
 *******************************************************************************/

package com.pushtechnology.diffusion.examples;

import static com.pushtechnology.diffusion.client.Diffusion.newTopicSpecification;
import static java.lang.String.format;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import com.pushtechnology.diffusion.client.Diffusion;
import com.pushtechnology.diffusion.client.features.TopicUpdate;
import com.pushtechnology.diffusion.client.features.control.topics.views.TopicViews;
import com.pushtechnology.diffusion.client.session.Session;
import com.pushtechnology.diffusion.client.topics.details.TopicType;
import com.pushtechnology.diffusion.datatype.json.JSON;
import com.pushtechnology.diffusion.datatype.json.JSONDataType;

/**
 * This example shows a control client creating a topic view that uses secondary
 * keys.
 * <p>
 * It presents a simplified sportsbook and uses a topic view to create a
 * secondary key on the country sporting events take place in.
 *
 * @author DiffusionData Limited
 * @since 6.3
 */
public final class CreateSecondaryKeys implements AutoCloseable {
    private final Session session;
    private final TopicUpdate topicUpdate;
    private final TopicViews topicViews;
    private final JSONDataType jsonDataType = Diffusion.dataTypes().json();
    private final AtomicInteger idGenerator = new AtomicInteger(0);

    /**
     * Constructor.
     *
     * @param serverUrl for example "ws://diffusion.example.com:80"
     */
    public CreateSecondaryKeys(String serverUrl) {
        session = Diffusion
            .sessions()
            .principal("control")
            .password("password")
            .open(serverUrl);

        topicUpdate = session.feature(TopicUpdate.class);
        topicViews = session.feature(TopicViews.class);
    }

    /**
     * Create a football event.
     *
     * @param country the country the event is in
     * @param homeTeam the home team
     * @param awayTeam the away team
     * @return future representing the creation of the event
     */
    public CompletableFuture<?> createEvent(String country, String homeTeam, String awayTeam) {
        // Create a path for the event containing a unique ID
        final String path = format("Football/All/%d", idGenerator.getAndIncrement());
        // Format the initial value describing the event
        final JSON value = jsonDataType.fromJsonString(format(
            "{\"country\":\"%s\",\"home\":\"%s\",\"away\":\"%s\"}",
            country,
            homeTeam,
            awayTeam));
        return topicUpdate.addAndSet(path, newTopicSpecification(TopicType.JSON), JSON.class, value);
    }

    /**
     * Create a secondary key that groups events by country the event is in.
     *
     * @return future representing the creation of the secondary keys
     */
    public CompletableFuture<?> createSecondaryKeyByCountry() {
        // This creates a single topic view that selects all football events
        // and maps them to a path containing their country, preserving the ID
        // generated for the topic
        return topicViews
            .createTopicView(
                "football-by-country",
                "map '?Football/All/' " +
                    "to 'Football/<scalar(/country)>/<path(2)>'");
    }

    @Override
    public void close() {
        session.close();
    }
}
