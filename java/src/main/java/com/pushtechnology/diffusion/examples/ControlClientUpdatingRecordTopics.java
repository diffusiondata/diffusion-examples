/*******************************************************************************
 * Copyright (C) 2016 Push Technology Ltd.
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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.pushtechnology.diffusion.client.Diffusion;
import com.pushtechnology.diffusion.client.callbacks.ErrorReason;
import com.pushtechnology.diffusion.client.callbacks.Registration;
import com.pushtechnology.diffusion.client.content.Content;
import com.pushtechnology.diffusion.client.content.ContentFactory;
import com.pushtechnology.diffusion.client.content.Record;
import com.pushtechnology.diffusion.client.content.Record.StructuredBuilder;
import com.pushtechnology.diffusion.client.content.RecordContentBuilder;
import com.pushtechnology.diffusion.client.content.metadata.MRecord;
import com.pushtechnology.diffusion.client.content.metadata.MetadataFactory;
import com.pushtechnology.diffusion.client.content.update.ContentUpdateFactory;
import com.pushtechnology.diffusion.client.features.control.topics.TopicControl;
import com.pushtechnology.diffusion.client.features.control.topics.TopicControl.AddContextCallback;
import com.pushtechnology.diffusion.client.features.control.topics.TopicControl.RemoveCallback;
import com.pushtechnology.diffusion.client.features.control.topics.TopicControl.RemoveContextCallback;
import com.pushtechnology.diffusion.client.features.control.topics.TopicUpdateControl;
import com.pushtechnology.diffusion.client.features.control.topics.TopicUpdateControl.Updater.UpdateContextCallback;
import com.pushtechnology.diffusion.client.session.Session;
import com.pushtechnology.diffusion.client.topics.details.RecordTopicDetails;

/**
 * An example of using a control client to create and update a record topic in
 * exclusive mode.
 * <P>
 * This uses the 'TopicControl' feature to create a topic and the
 * 'TopicUpdateControl' feature to send updates to it.
 * <P>
 * Both 'full' and 'delta' updating techniques are demonstrated. Full updates
 * involve sending the whole topic state to the server where it will be compared
 * with the current state and a delta of any differences published to subscribed
 * clients. With delta updates it is only necessary to send the values of the
 * fields that have changed to the server where they will be applied to the
 * current topic state and published to subscribers. The latter mechanism is not
 * so well suited to this example where there are only two fields but for topics
 * with many fields this could represent considerable savings in the amount of
 * data sent to the server.
 * <P>
 * To send updates to a topic, the client session requires the 'update_topic'
 * permission for that branch of the topic tree.
 * <P>
 * The example also demonstrates a simple usage of a structured record builder
 * for generating content as such a builder validates the input against the
 * metadata.
 *
 * @author Push Technology Limited
 * @since 5.7
 * @see ClientConsumingRecordTopics
 */
public final class ControlClientUpdatingRecordTopics {

    private static final String ROOT_TOPIC = "FX";

    private final CountDownLatch closeLatch = new CountDownLatch(2);
    private final Session session;
    private final TopicControl topicControl;
    private final MRecord recordMetadata;
    private final RecordTopicDetails topicDetails;
    private final ContentUpdateFactory updateFactory;
    private final StructuredBuilder deltaRecordBuilder;
    private volatile Registration updateSourceRegistration;
    private volatile TopicUpdateControl.Updater topicUpdater;

    /**
     * Constructor.
     *
     * @param serverUrl for example "ws://diffusion.example.com:80"
     */
    public ControlClientUpdatingRecordTopics(String serverUrl) {

        session =
            Diffusion.sessions().principal("control").password("password")
                .open(serverUrl);

        topicControl = session.feature(TopicControl.class);

        final MetadataFactory mf = Diffusion.metadata();

        // Create the record metadata for the rates topic. It has two decimal
        // fields which are maintained to 5 decimal places and allow empty
        // values
        recordMetadata =
            mf.recordBuilder("Rates")
                .add(
                    mf.decimalBuilder("Bid").scale(5).allowsEmpty(true).build())
                .add(
                    mf.decimalBuilder("Ask").scale(5).allowsEmpty(true).build())
                .build();

        // Create the topic details to be used for all rates topics
        topicDetails =
            topicControl.newDetailsBuilder(RecordTopicDetails.Builder.class)
                .emptyFieldValue(Record.EMPTY_FIELD_STRING)
                .metadata(mf.content("CurrencyDetails", recordMetadata))
                .build();

        // Create a delta builder that can be reused for bid only changes
        deltaRecordBuilder =
            Diffusion.content().newDeltaRecordBuilder(recordMetadata).
                emptyFieldValue(Record.EMPTY_FIELD_STRING);

        final TopicUpdateControl updateControl =
            session.feature(TopicUpdateControl.class);

        updateFactory = updateControl.updateFactory(ContentUpdateFactory.class);

        // Register as an updater for all topics under the root
        updateControl.registerUpdateSource(
            ROOT_TOPIC,
            new TopicUpdateControl.UpdateSource.Default() {
                @Override
                public void onRegistered(String topicPath, Registration registration) {
                    updateSourceRegistration = registration;
                }

                @Override
                public void onActive(String topicPath, TopicUpdateControl.Updater updater) {
                    topicUpdater = updater;
                }

                @Override
                public void onClose(String topicPath) {
                    closeLatch.countDown();
                }

                @Override
                public void onError(String topicPath, ErrorReason errorReason) {
                    super.onError(topicPath, errorReason);
                    closeLatch.countDown();
                }
            });

    }

    /**
     * Adds a new conversion rate in terms of base currency and target currency.
     *
     * The bid and ask rates are entered as strings which may be a decimal value
     * which will be parsed and validated, rounding to 5 decimal places. If ""
     * (zero length string) is supplied, the rate will be set to 'empty' and
     * clients will receive a zero length string in the initial load.
     *
     * @param currency the base currency (e.g. GBP)
     *
     * @param targetCurrency the target currency (e.g. USD)
     *
     * @param bid the bid rate
     *
     * @param ask the ask rate
     *
     * @param callback a callback which will be called to report the outcome.
     *        The context in the callback will be currency/targetCurrency (e.g.
     *        "GBP/USD")
     */
    public void addRate(
        String currency,
        String targetCurrency,
        String bid,
        String ask,
        AddContextCallback<String> callback) {

        topicControl.addTopic(
            rateTopicName(currency, targetCurrency),
            topicDetails,
            createRateContent(bid, ask),
            String.format("%s/%s", currency, targetCurrency),
            callback);
    }

    /**
     * Update a rate.
     * <P>
     * The rate in question must have been added first using {@link #addRate}
     * otherwise this will fail.
     * <P>
     * The bid and ask rates are entered as strings which may be a decimal value
     * which will be parsed and validated, rounding to 5 decimal places. A zero
     * length string may be supplied to indicate 'no rate available'. The server
     * will compare the supplied values with the current values and if different
     * will notify clients of a delta of change. Only changed fields are
     * notified to clients, unchanged fields are passed as zero length string.
     * If a field has changed to zero length then the client will receive the
     * special empty field value in the delta.
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
    public void changeRate(
        String currency,
        String targetCurrency,
        String bid,
        String ask,
        UpdateContextCallback<String> callback) {

        if (topicUpdater == null) {
            throw new IllegalStateException("Not registered as updater");
        }

        topicUpdater.update(
            rateTopicName(currency, targetCurrency),
            updateFactory.update(createRateContent(bid, ask)),
            String.format("%s/%s", currency, targetCurrency),
            callback);

    }

    /**
     * Updates just the 'bid' value for a specified rate.
     * <P>
     * This method demonstrates the alternative 'delta' mechanism of updating.
     * In this example it does not make much sense but for records with many
     * fields where you know only one is changing this removes the need to send
     * the whole topic state in each update.
     *
     * @param currency the base currency
     *
     * @param targetCurrency the target currency
     *
     * @param bid the new bid rate which can be "" to set to 'not available'
     *
     * @param callback a callback which will be called to report the outcome.
     *        The context in the callback will be currency/targetCurrency (e.g.
     *        "GBP/USD")
     */
    public void changeBidRate(
        String currency,
        String targetCurrency,
        String bid,
        UpdateContextCallback<String> callback) {

        if (topicUpdater == null) {
            throw new IllegalStateException("Not registered as updater");
        }

        final ContentFactory cf = Diffusion.content();

        topicUpdater.update(
            rateTopicName(currency, targetCurrency),
            updateFactory.apply(
                cf.newBuilder(RecordContentBuilder.class)
                    .putRecords(
                        deltaRecordBuilder.set(
                            "Bid",
                            "".equals(bid) ? Record.EMPTY_FIELD_STRING : bid)
                            .build())
                    .build()),
            String.format("%s/%s", currency, targetCurrency),
            callback);

    }

    /**
     * Remove a rate (removes its topic).
     *
     * @param currency the base currency
     *
     * @param targetCurrency the target currency
     *
     * @param callback reports the outcome
     */
    public void removeRate(
        String currency,
        String targetCurrency,
        RemoveContextCallback<String> callback) {

        topicControl.removeTopics(
            rateTopicName(currency, targetCurrency),
            String.format("%s/%s", currency, targetCurrency),
            callback);
    }

    /**
     * Removes a currency (removes its topic and all subordinate rate topics).
     *
     * @param currency the base currency
     *
     * @param callback reports the outcome
     */
    public void removeCurrency(
        String currency,
        RemoveContextCallback<String> callback) {

        topicControl.removeTopics(
            String.format("%s/%s", ROOT_TOPIC, currency),
            currency,
            callback);
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

        // Remove our topics and close session when done
        topicControl.removeTopics(
            ROOT_TOPIC,
            new RemoveCallback() {
                @Override
                public void onDiscard() {
                    closeLatch.countDown();
                }

                @Override
                public void onTopicsRemoved() {
                    closeLatch.countDown();
                }
            });

        try {
            closeLatch.await(5, TimeUnit.SECONDS);
        }
        finally {
            session.close();
        }
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
    private static String rateTopicName(String currency, String targetCurrency) {
        return String.format("%s/%s/%s", ROOT_TOPIC, currency, targetCurrency);
    }

    /**
     * Create rate contents for a full update.
     *
     * @param bid the bid rate or ""
     * @param ask the ask rate or ""
     */
    private Content createRateContent(String bid, String ask) {
        final ContentFactory cf = Diffusion.content();

        final Content content =
            cf.newBuilder(RecordContentBuilder.class)
                .putRecords(
                    cf.newRecordBuilder(recordMetadata)
                        .set("Bid", bid)
                        .set("Ask", ask)
                        .build())
                .build();

        return content;
    }

}
