/*******************************************************************************
 * Copyright (C) 2016, 2023 DiffusionData Ltd.
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

import static java.lang.Math.max;
import static java.util.Collections.unmodifiableSortedMap;

import java.io.IOException;
import java.time.Instant;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Consumer;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;
import com.pushtechnology.diffusion.client.callbacks.ErrorReason;
import com.pushtechnology.diffusion.client.features.TimeSeries;
import com.pushtechnology.diffusion.client.features.TimeSeries.Event;
import com.pushtechnology.diffusion.client.features.TimeSeries.Query;
import com.pushtechnology.diffusion.client.features.Topics;
import com.pushtechnology.diffusion.client.features.Topics.ValueStream;
import com.pushtechnology.diffusion.client.session.Session;
import com.pushtechnology.diffusion.client.topics.details.TopicSpecification;
import com.pushtechnology.diffusion.datatype.json.JSON;

/**
 * Demonstrate the TimeSeries API.
 *
 * <p>
 * This demonstrates client-side view maintenance for a simple chat application.
 *
 * <p>
 * The view is modeled by the {@link ChatView} class. This has a start time, and
 * a map of sequence number to messages that have occurred after the start time.
 *
 * <p>
 * The {@link #subscribeChatView subscribeChatView} method subscribes a session
 * to a time series topic of chat messages stored as JSON objects. ChatView
 * models each chat message using the {@link ChatMessage} class. Once
 * subscribed, the ChatView will be asynchronously updated with new messages and
 * edits to existing messages.
 *
 * @author DiffusionData Limited
 * @since 6.0
 */
public final class TimeSeriesQueryExample {
    /** Jackson ObjectMapper used by {@link #jsonToChat(JSON)}. */
    private static final ObjectMapper CBOR_MAPPER =
        new ObjectMapper(new CBORFactory());

    private TimeSeriesQueryExample() {
    }

    /**
     * Connect a ChatView to a time series topic.
     *
     * @param chatView the ChatView
     * @param chatTopicPath path of a JSON time series topic storing chat
     *        messages
     * @param errorHandler called if an operation fails
     */
    public static void subscribeChatView(
        Session session,
        ChatView chatView,
        String chatTopicPath,
        Consumer<Throwable> errorHandler) {

        final Topics topics = session.feature(Topics.class);

        final ValueStream<Event<JSON>> subscriptionStream =
            new ValueStream.Default<Event<JSON>>() {
                private volatile boolean initialValue = true;

                @Override
                public void onValue(
                    String topicPath,
                    TopicSpecification specification,
                    Event<JSON> oldEvent,
                    Event<JSON> event) {

                    // When the subscription initially completes, the stream
                    // will receive an initial range of events from the server.
                    // If there is a gap between the latest event processed by
                    // the ChatView and the first event received, query the
                    // time series topic to retrieve the missing events.
                    if (initialValue &&
                        event.sequence() > chatView.expectedNextSequence()) {

                        initialValue = false;

                        final Query<JSON> query = chatView.missingEventQuery(
                            session.feature(TimeSeries.class),
                            event.sequence());

                        query
                            .selectFrom(chatTopicPath)
                            .whenComplete((result, e) -> {
                                if (e != null) {
                                    topics.removeStream(this);
                                    errorHandler.accept(e);
                                }
                                else {
                                    result.stream().forEach(chatView::addEvent);
                                }
                            });
                    }

                    chatView.addEvent(event);
                }

            @Override
            public void onError(ErrorReason errorReason) {
                errorHandler.accept(new RuntimeException(
                    "Subscription stream failed: " + errorReason));
            }
        };

        topics.addTimeSeriesStream(chatTopicPath, JSON.class, subscriptionStream);

        topics.subscribe(chatTopicPath)
            .whenComplete((result, e) -> {
                if (e != null) {
                    topics.removeStream(subscriptionStream);
                    errorHandler.accept(e);
                }
            });
    }

    /**
     * A client-side model of a time series of ChatMessages.
     */
    public static class ChatView {

        private final Instant startOfView;
        private final SortedMap<Long, Event<ChatMessage>> messages =
            new TreeMap<>();

        private long latestSequence = -1;

        /**
         * Constructor.
         *
         * @param startOfView the start of the view
         */
        public ChatView(Instant startOfView) {
            this.startOfView = startOfView;
        }

        /**
         * @return an ordered map of sequence number -> chat message events
         */
        public SortedMap<Long, Event<ChatMessage>> getMessages() {
            return unmodifiableSortedMap(messages);
        }

        private synchronized void addEvent(Event<JSON> event) {
            if (event.timestamp() >= startOfView.toEpochMilli()) {
                messages.put(
                    event.originalEvent().sequence(),
                    event.withValue(jsonToChat(event.value())));
            }

            latestSequence = max(latestSequence, event.sequence());
        }

        private synchronized long expectedNextSequence() {
            return latestSequence + 1;
        }

        /**
         * @return a query configured to return all events that affect the view
         *         from the next expected event until receivedSequence
         */
        private synchronized Query<JSON> missingEventQuery(
            TimeSeries timeSeries,
            long receivedSequence) {

            return timeSeries.rangeQuery()
                .from(startOfView)
                .editRange().from(latestSequence + 1)
                .to(receivedSequence - 1)
                .as(JSON.class);
        }
    }

    /**
     * Simple model of a chat message that combines application metadata
     * (priority, senderId) with a textual message.
     */
    public static class ChatMessage {
        private final String text;
        private final int priority;
        private final int senderId;

        /**
         * Constructor.
         */
        @JsonCreator
        public ChatMessage(
            @JsonProperty("text") String text,
            @JsonProperty("priority") int priority,
            @JsonProperty("senderId") int senderId) {
            this.text = text;
            this.priority = priority;
            this.senderId = senderId;
        }


        /**
         * @return the message text.
         */
        public String getText() {
            return text;
        }

        /**
         * @return a priority code; high priority messages may be rendered
         *         differently
         */
        public int getPriority() {
            return priority;
        }

        /**
         * Used by the send to de-duplicate pending messages on reconnection.
         */
        public int getSenderId() {
            return senderId;
        }
    }

    /**
     * Use the third-party Jackson library to parse a JSON message as a
     * ChatMessage.
     */
    private static ChatMessage jsonToChat(JSON value) {
        try {
            return CBOR_MAPPER.readValue(value.asInputStream(), ChatMessage.class);
        }
        catch (IOException e) {
            throw new RuntimeException("Failed to parse event as chat message", e);
        }
    }
}
