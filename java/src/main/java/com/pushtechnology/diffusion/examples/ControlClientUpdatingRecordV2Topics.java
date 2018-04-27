/*******************************************************************************
 * Copyright (C) 2017, 2018 Push Technology Ltd.
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

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.pushtechnology.diffusion.client.Diffusion;
import com.pushtechnology.diffusion.client.callbacks.Registration;
import com.pushtechnology.diffusion.client.features.control.topics.TopicControl;
import com.pushtechnology.diffusion.client.features.control.topics.TopicUpdateControl;
import com.pushtechnology.diffusion.client.features.control.topics.TopicUpdateControl.Updater.UpdateContextCallback;
import com.pushtechnology.diffusion.client.session.Session;
import com.pushtechnology.diffusion.client.topics.details.TopicSpecification;
import com.pushtechnology.diffusion.client.topics.details.TopicType;
import com.pushtechnology.diffusion.datatype.recordv2.RecordV2;
import com.pushtechnology.diffusion.datatype.recordv2.RecordV2DataType;
import com.pushtechnology.diffusion.datatype.recordv2.model.MutableRecordModel;
import com.pushtechnology.diffusion.datatype.recordv2.schema.Schema;

/**
 * An example of using a control client to create and update a RecordV2 topic in
 * exclusive mode.
 * <P>
 * This uses the 'TopicControl' feature to create a topic and the
 * 'TopicUpdateControl' feature to send updates to it.
 * <P>
 * To send updates to a topic, the client session requires the 'update_topic'
 * permission for that branch of the topic tree.
 * <P>
 * The example can be used with or without the use of a schema. This is simply
 * to demonstrate the different mechanisms and is not necessarily demonstrating
 * the most efficient way to update such a topic.
 *
 * @author Push Technology Limited
 * @since 6.0
 * @see ClientConsumingRecordV2Topics
 */
public final class ControlClientUpdatingRecordV2Topics {

    private static final String ROOT_TOPIC = "FX";

    private final Session session;
    private final TopicControl topicControl;
    private final TopicSpecification topicSpecification;
    private volatile Registration updateSourceRegistration;
    private final Schema schema;
    private final RecordV2DataType dataType;

    private volatile TopicUpdateControl.ValueUpdater<RecordV2> valueUpdater;

    /**
     * Constructor.
     *
     * @param serverUrl for example "ws://diffusion.example.com:80"
     */
    public ControlClientUpdatingRecordV2Topics(
        String serverUrl,
        boolean withSchema)
            throws InterruptedException, ExecutionException, TimeoutException {

        session =
            Diffusion.sessions().principal("control").password("password")
                .open(serverUrl);

        topicControl = session.feature(TopicControl.class);

        // Create the root topic that will remove itself when the session closes
        final TopicSpecification specification =
            topicControl.newSpecification(TopicType.STRING).withProperty(
                TopicSpecification.REMOVAL,
                "When no session has '$SessionId is \"" +
                session.getSessionId().toString() +
                "\"' remove '" +
                "?" + ROOT_TOPIC + "//'");
        topicControl.addTopic(ROOT_TOPIC, specification).get(5, TimeUnit.SECONDS);

        dataType = Diffusion.dataTypes().recordV2();

        if (withSchema) {
            schema = dataType.schemaBuilder()
                .record("Rates").decimal("Bid", 5).decimal("Ask", 5).build();
            // Create the topic specification to be used for all rates topics
            topicSpecification =
                topicControl.newSpecification(TopicType.RECORD_V2)
                    .withProperty(
                        TopicSpecification.SCHEMA,
                        schema.asJSONString());
        }
        else {
            schema = null;
            // Create the topic specification to be used for all rates topics
            topicSpecification =
                topicControl.newSpecification(TopicType.RECORD_V2);
        }

        final TopicUpdateControl updateControl =
            session.feature(TopicUpdateControl.class);

        // Register as an updater for all topics under the root
        updateControl.registerUpdateSource(
            ROOT_TOPIC,
            new TopicUpdateControl.UpdateSource.Default() {
                @Override
                public void onRegistered(
                    String topicPath,
                    Registration registration) {
                    updateSourceRegistration = registration;
                }

                @Override
                public void onActive(
                    String topicPath,
                    TopicUpdateControl.Updater updater) {
                    valueUpdater = updater.valueUpdater(RecordV2.class);
                }
            });

    }

    /**
     * Adds a new conversion rate in terms of base currency and target currency.
     *
     * The bid and ask rates are entered as strings which may be a decimal value
     * which will be parsed and validated, rounding to 5 decimal places.
     *
     * @param currency the base currency (e.g. GBP)
     *
     * @param targetCurrency the target currency (e.g. USD)
     */
    public void addRateTopic(
        String currency,
        String targetCurrency)
        throws InterruptedException, ExecutionException, TimeoutException {

        topicControl.addTopic(
            rateTopicName(currency, targetCurrency),
            topicSpecification).get(5, TimeUnit.SECONDS);
    }

    /**
     * Set a rate.
     * <P>
     * The rate topic in question must have been added first using
     * {@link #addRateTopic} otherwise this will fail.
     *
     * @param currency the base currency
     *
     * @param targetCurrency the target currency
     *
     * @param bid the new bid rate
     *
     * @param ask the new ask rate
     *
     * @param callback a callback which will be called to report the outcome.
     *        The context in the callback will be currency/targetCurrency (e.g.
     *        "GBP/USD")
     */
    public void setRate(
        String currency,
        String targetCurrency,
        String bid,
        String ask,
        UpdateContextCallback<String> callback) {

        if (valueUpdater == null) {
            throw new IllegalStateException("Not registered as updater");
        }

        final RecordV2 value;
        if (schema == null) {
            value = dataType.valueBuilder().addFields(bid, ask).build();
        }
        else {
            // Mutable models could be kept and reused but for this simple
            // example one is created every time
            final MutableRecordModel model =
                schema.createMutableModel();
            model.set("Bid", bid);
            model.set("Ask", ask);
            value = model.asValue();
        }

        valueUpdater.update(
            rateTopicName(currency, targetCurrency),
            value,
            String.format("%s/%s", currency, targetCurrency),
            callback);

    }

    /**
     * Remove a rate (removes its topic).
     *
     * @param currency the base currency
     *
     * @param targetCurrency the target currency
     */
    public void removeRate(
        String currency,
        String targetCurrency)
        throws InterruptedException, ExecutionException, TimeoutException {

        topicControl.removeTopics(
            rateTopicName(currency, targetCurrency))
            .get(5, TimeUnit.SECONDS);
    }

    /**
     * Removes a currency (removes its topic and all subordinate rate topics).
     *
     * @param currency the base currency
     */
    public void removeCurrency(String currency)
        throws InterruptedException, ExecutionException, TimeoutException {
        topicControl
            .removeTopics(String.format("?%s/%s//", ROOT_TOPIC, currency))
            .get(5, TimeUnit.SECONDS);
    }

    /**
     * Close the session.
     */
    public void close() throws InterruptedException {
        // Close the registered update source
        final Registration registration = this.updateSourceRegistration;
        if (registration != null) {
            registration.close();
        }
        session.close();
    }

    /**
     * Generates a hierarchical topic name for a rate topic.
     * <P>
     * e.g. for currency=GBP and targetCurrency=USD would return "FX/GBP/USD".
     *
     * @param currency the base currency
     * @param targetCurrency the target currency
     * @return the topic name
     */
    private static String rateTopicName(String currency,
        String targetCurrency) {
        return String.format("%s/%s/%s", ROOT_TOPIC, currency, targetCurrency);
    }

}
