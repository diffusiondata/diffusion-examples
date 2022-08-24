/*******************************************************************************
 * Copyright (C) 2016, 2021 Push Technology Ltd.
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

import static com.pushtechnology.diffusion.client.Diffusion.newTopicSpecification;
import static com.pushtechnology.diffusion.client.topics.details.TopicSpecification.REMOVAL;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import com.pushtechnology.diffusion.client.Diffusion;
import com.pushtechnology.diffusion.client.features.TopicUpdate;
import com.pushtechnology.diffusion.client.features.control.topics.TopicControl;
import com.pushtechnology.diffusion.client.session.Session;
import com.pushtechnology.diffusion.client.topics.details.TopicSpecification;
import com.pushtechnology.diffusion.client.topics.details.TopicType;
import com.pushtechnology.diffusion.datatype.json.JSON;
import com.pushtechnology.diffusion.datatype.json.JSONDataType;

/**
 * This example shows a control client creating a JSON topic and sending updates
 * to it.
 * <P>
 * There will be a topic for each currency for which rates are provided. The
 * topic will be created under the FX topic - so, for example FX/GBP will
 * contain a map of all rate conversions from the base GBP currency. The rates
 * are represented as string decimal values (e.g. "12.457").
 * <P>
 * Topic values are represented in JSON format e.g. {"USD":"123.45","HKD":"456.3"}
 * for which the corresponding JSONString would be "{\"USD\":\"123.45\",\"HKD\":\"456.3\"}"
 * <P>
 * The {@code addRates} method shows how to create a new rates topic, specifying
 * its initial values in a JSON string.
 * <P>
 * The {@code changeRates} method which takes a JSON string shows how to completely
 * replace the set of rates for a currency with new values.
 * <P>
 * After the first usage of changeRates for any topic the value is cached,
 * and so subsequent set calls can compare with the last value
 * and send only the differences to the server.
 *
 * @author Push Technology Limited
 * @since 5.7
 * @see ClientConsumingJSONTopics
 */
public final class ControlClientUpdatingJSONTopics {

    private static final String ROOT_TOPIC = "FX";

    private final Session session;
    private final TopicControl topicControl;
    private final TopicUpdate topicUpdate;
    private final TopicSpecification topicSpecification;
    private final JSONDataType jsonDataType = Diffusion.dataTypes().json();

    /**
     * Constructor.
     *
     * @param serverUrl for example "ws://diffusion.example.com:80"
     */
    public ControlClientUpdatingJSONTopics(String serverUrl)
        throws InterruptedException, ExecutionException, TimeoutException {

        session =
            Diffusion.sessions().principal("control").password("password")
                .open(serverUrl);

        topicControl = session.feature(TopicControl.class);
        topicUpdate = session.feature(TopicUpdate.class);
        topicSpecification = newTopicSpecification(TopicType.JSON);



        // Create the root topic that will remove itself when the session closes
        final TopicSpecification specification =
            topicSpecification
                .withProperty(REMOVAL,
                "When this session closes remove '?" + ROOT_TOPIC + "//'");

        topicControl.addTopic(ROOT_TOPIC, specification).get(5, SECONDS);
    }

    /**
     * Add a new rates topic and sets initial values.
     *
     * @param currency the base currency
     * @param value JSON string of initial rates values
     */
    public void addRates(
        String currency,
        String value) {
        topicUpdate
        .addAndSet(rateTopicName(currency),
            topicSpecification,
            JSON.class,
            jsonDataType.fromJsonString(value));
    }

    /**
     * Update an existing rates topic.
     *
     * @param currency the base currency
     * @param jsonString a JSON string specifying the map of currency rates
     * @return a CompletableFuture that completes when a response is received
     *         from the server
     */
    public CompletableFuture<?> changeRates(
        String currency,
        String jsonString)
        throws IllegalArgumentException {

        return topicUpdate.set(
            rateTopicName(currency),
            JSON.class,
            jsonDataType.fromJsonString(jsonString));
    }

    /**
     * Remove a rates entry (removes its topic).
     *
     * @param currency the currency
     *
     * @return a CompletableFuture that completes when a response is received
     *         from the server
     */
    public CompletableFuture<?> removeRates(String currency) {

        final String topicName = rateTopicName(currency);

        return topicControl.removeTopics(topicName);
    }

    /**
     * Close the session.
     */
    public void close() {
        session.close();
    }

    /**
     * Generate a hierarchical topic name for a rates topic.
     * <P>
     * e.g. for currency=GBP would return "FX/GBP".
     *
     * @param currency the currency
     * @return the topic name
     */
    private static String rateTopicName(String currency) {
        return String.format("%s/%s", ROOT_TOPIC, requireNonNull(currency));
    }

}
