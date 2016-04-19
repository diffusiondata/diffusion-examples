/*******************************************************************************
 * Copyright (C) 2015, 2016 Push Technology Ltd.
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

import static java.util.Objects.requireNonNull;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import com.pushtechnology.diffusion.client.Diffusion;
import com.pushtechnology.diffusion.client.content.Content;
import com.pushtechnology.diffusion.client.content.Record;
import com.pushtechnology.diffusion.client.content.RecordContentReader;
import com.pushtechnology.diffusion.client.content.metadata.MRecord;
import com.pushtechnology.diffusion.client.content.metadata.MetadataFactory;
import com.pushtechnology.diffusion.client.features.Topics;
import com.pushtechnology.diffusion.client.features.Topics.TopicStream;
import com.pushtechnology.diffusion.client.features.Topics.UnsubscribeReason;
import com.pushtechnology.diffusion.client.session.Session;
import com.pushtechnology.diffusion.client.types.UpdateContext;
import com.pushtechnology.diffusion.client.types.UpdateType;

/**
 * This demonstrates a client consuming record topics and reading the content
 * using a StructuredReader.
 * <P>
 * This makes use of the 'Topics' feature only.
 * <P>
 * To subscribe to a topic, the client session must have the 'select_topic'
 * and 'read_topic' permissions for that branch of the topic tree.
 * <P>
 * This example receives updates to currency conversion rates via a branch of
 * the topic tree where the root topic is called "FX" which under it has a topic
 * for each base currency and under each of those is a topic for each target
 * currency which contains the bid and ask rates. So a topic FX/GBP/USD would
 * contain the rates for GBP to USD.
 * <P>
 * This example maintains a local map of the rates and also notifies a listener
 * of any rates changes.
 * <P>
 * The example shows the use of empty fields. Any of the rates can be empty
 * (meaning the rate is not available in this example) and so can be "" (a zero
 * length string) in the topic value. Because delta updates use a zero length
 * string to indicate that a field has not changed, a special 'empty field'
 * value is used to indicate that the field has changed to empty in deltas. The
 * client application must therefore convert empty string values to "" for the
 * local rate value.
 *
 * @author Push Technology Limited
 * @since 5.7
 * @see ControlClientUpdatingRecordTopics
 */
public final class ClientConsumingRecordTopics {

    private static final String ROOT_TOPIC = "FX";

    /**
     * The map of currency codes to currency objects which each maintain rates
     * for each target currency.
     */
    private final Map<String, Currency> currencies = new ConcurrentHashMap<>();

    private final RatesListener listener;

    private final Session session;

    /**
     * Constructor.
     *
     * @param serverUrl for example "ws://diffusion.example.com:80"
     * @param listener a listener that will be notified of all rates and rate
     *        changes
     */
    public ClientConsumingRecordTopics(String serverUrl, RatesListener listener) {

        this.listener = requireNonNull(listener);

        session =
            Diffusion.sessions().principal("client").password("password")
                .open(serverUrl);

        // Create the record metadata for the rates topic. It has two decimal
        // fields which are maintained to 5 decimal places and allow empty
        // values
        final MetadataFactory mf = Diffusion.metadata();
        final MRecord recordMetadata =
            mf.recordBuilder("Rates")
                .add(mf.decimalBuilder("Bid").scale(5).allowsEmpty(true).build())
                .add(mf.decimalBuilder("Ask").scale(5).allowsEmpty(true).build())
                .build();

        // Use the Topics feature to add a topic stream and subscribe to all
        // topic under the root
        final Topics topics = session.feature(Topics.class);
        final String topicSelector = String.format("?%s//", ROOT_TOPIC);
        topics.addTopicStream(
            topicSelector,
            new RatesTopicStream(recordMetadata));
        topics.subscribe(
            topicSelector,
            new Topics.CompletionCallback.Default());
    }

    /**
     * Returns the rates for a given base and target currency.
     *
     * @param currency the base currency
     * @param targetCurrency the target currency
     * @return the rates or null if there is no such base or target currency
     */
    public Rates getRates(String currency, String targetCurrency) {
        final Currency currencyObject = currencies.get(currency);
        if (currencyObject != null) {
            return currencyObject.getRates(targetCurrency);
        }
        return null;
    }

    /**
     * This is used to apply topic stream updates to the local map.
     *
     * @param type the update may be a snapshot or a delta
     * @param currency the base currency
     * @param targetCurrency the target currency
     * @param bid the bid rate
     * @param ask the ask rate
     */
    private void applyUpdate(
        UpdateType type,
        String currency,
        String targetCurrency,
        String bid,
        String ask) {

        Currency currencyObject = currencies.get(currency);
        if (currencyObject == null) {
            currencyObject = new Currency();
            currencies.put(currency, currencyObject);
        }

        final Rates rates;
        if (type == UpdateType.SNAPSHOT) {
            rates = currencyObject.setRate(targetCurrency, bid, ask);
        }
        else {
            rates = currencyObject.updateRate(targetCurrency, bid, ask);
        }

        listener.onNewRate(
            currency, targetCurrency, rates.bidRate, rates.askRate);

    }

    /**
     * This is used by the topic stream when notified of the unsubscription from
     * a base currency topic.
     * <P>
     * It will remove the base currency and all its rates from the local map.
     *
     * @param currency the currency to remove
     */
    private void removeCurrency(String currency) {

        final Currency oldCurrency = currencies.remove(currency);
        for (String targetCurrency : oldCurrency.rates.keySet()) {
            listener.onRateRemoved(currency, targetCurrency);
        }
    }

    /**
     * This is used by the topic stream when notified of the unsubscription from
     * a target currency topic.
     * <P>
     * It will remove the rates for the target currency under the base currency.
     *
     * @param currency the base currency
     * @param targetCurrency the target currency
     */
    private void removeRate(
        String currency,
        String targetCurrency) {

        final Currency currencyObject = currencies.get(currency);
        if (currencyObject != null) {
            if (currencyObject.rates.remove(targetCurrency) != null) {
                listener.onRateRemoved(currency, targetCurrency);
            }
        }
    }

    /**
     * Close session.
     */
    public void close() {
        currencies.clear();
        session.close();
    }

    /**
     * Encapsulates a base currency and all of its known rates.
     */
    private static class Currency {

        private final Map<String, Rates> rates = new HashMap<>();

        private Rates getRates(String currency) {
            return rates.get(currency);
        }

        private Rates setRate(String currency, String bid, String ask) {
            final Rates newRates = new Rates(bid, ask);
            rates.put(currency, newRates);
            return newRates;
        }

        private Rates updateRate(String currency, String bid, String ask) {
            final Rates newRates = rates.get(currency).update(bid, ask);
            rates.put(currency, newRates);
            return newRates;
        }

    }

    /**
     * Encapsulates the rates for a particular base/target currency pair.
     */
    public static final class Rates {

        private final String bidRate;
        private final String askRate;

        /**
         * Constructor.
         *
         * @param bid the bid rate or ""
         * @param ask the ask rate or ""
         */
        private Rates(String bid, String ask) {
            bidRate = bid;
            askRate = ask;
        }

        /**
         * Applies updated values to this instance to produce a new Rates
         * instance.
         *
         * @param bid the new bid rate or "" if it has not changed or the empty
         *        field value if the rate is now unavailable
         * @param ask the new ask rate or "" if it has not changed or the empty
         *        field value if the rate is now unavailable
         * @return new Rates
         */
        private Rates update(String bid, String ask) {

            final String newBid;
            if ("".equals(bid)) {
                newBid = bidRate;
            }
            else if (Record.EMPTY_FIELD_STRING.equals(bid)) {
                newBid = "";
            }
            else {
                newBid = bid;
            }

            final String newAsk;
            if ("".equals(ask)) {
                newAsk = askRate;
            }
            else if (Record.EMPTY_FIELD_STRING.equals(ask)) {
                newAsk = "";
            }
            else {
                newAsk = ask;
            }

            return new Rates(newBid, newAsk);
        }

        /**
         * Returns the bid rate.
         *
         * @return bid rate or "" if not available
         */
        public String getBidRate() {
            return bidRate;
        }

        /**
         * Returns the ask rate.
         *
         * @return ask rate or "" if not available
         */
        public String getAskRate() {
            return askRate;
        }

    }

    /**
     * A listener for Rates updates.
     */
    public interface RatesListener {

        /**
         * Notification of a new rate or rate update.
         *
         * @param currency the base currency
         * @param targetCurrency the target currency
         * @param bid the bid rate or "" if not available
         * @param ask the ask rate or "" if not available
         */
        void onNewRate(String currency, String targetCurrency, String bid,
            String ask);

        /**
         * Notification of a rate being removed.
         *
         * @param currency the base currency
         * @param targetCurrency the target currency
         */
        void onRateRemoved(String currency, String targetCurrency);
    }

    /**
     * The topic stream for all updates.
     */
    private final class RatesTopicStream extends TopicStream.Default {

        private final MRecord metadata;

        private RatesTopicStream(MRecord metadata) {
            this.metadata = metadata;
        }

        @Override
        public void onTopicUpdate(
            String topic,
            Content content,
            UpdateContext context) {

            final String[] topicElements = topic.split("/");
            // It is only a rate update if topic name has 3 elements in path
            if (topicElements.length == 3) {
                final Record record =
                    Diffusion.content().newReader(
                        RecordContentReader.class, content).nextRecord();
                final Record.StructuredReader reader =
                    record.newReader(metadata, Record.EMPTY_FIELD_STRING);
                applyUpdate(
                    context.getUpdateType(),
                    topicElements[1], // The base currency
                    topicElements[2], // The target currency
                    reader.get("Bid"), // The bid rate
                    reader.get("Ask")); // The ask rate
            }
        }

        @Override
        public void onUnsubscription(
            String topicPath,
            UnsubscribeReason reason) {
            final String[] topicElements = topicPath.split("/");
            if (topicElements.length == 3) {
                removeRate(topicElements[1], topicElements[2]);
            }
            else if (topicElements.length == 2) {
                removeCurrency(topicElements[1]);
            }
        }

    }

}
