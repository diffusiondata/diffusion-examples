/*******************************************************************************
 * Copyright (C) 2017 Push Technology Ltd.
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
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pushtechnology.diffusion.client.Diffusion;
import com.pushtechnology.diffusion.client.features.Topics;
import com.pushtechnology.diffusion.client.features.Topics.UnsubscribeReason;
import com.pushtechnology.diffusion.client.features.Topics.ValueStream;
import com.pushtechnology.diffusion.client.session.Session;
import com.pushtechnology.diffusion.client.topics.details.TopicSpecification;
import com.pushtechnology.diffusion.datatype.recordv2.RecordV2;
import com.pushtechnology.diffusion.datatype.recordv2.RecordV2Delta;
import com.pushtechnology.diffusion.datatype.recordv2.RecordV2Delta.Change;
import com.pushtechnology.diffusion.datatype.recordv2.model.RecordModel;
import com.pushtechnology.diffusion.datatype.recordv2.schema.Schema;
import com.pushtechnology.diffusion.datatype.recordv2.schema.SchemaParseException;

/**
 * This demonstrates a client consuming RecordV2 topics.
 * <P>
 * It has been contrived to demonstrate the various techniques for Diffusion
 * record topics and is not necessarily realistic or efficient in its
 * processing.
 * <P>
 * It can be run using a schema or not using a schema and demonstrates how the
 * processing could be done in both cases.
 * <P>
 * This makes use of the 'Topics' feature only.
 * <P>
 * To subscribe to a topic, the client session must have the 'select_topic' and
 * 'read_topic' permissions for that branch of the topic tree.
 * <P>
 * This example receives updates to currency conversion rates via a branch of
 * the topic tree where the root topic is called "FX" which under it has a topic
 * for each base currency and under each of those is a topic for each target
 * currency which contains the bid and ask rates. So a topic FX/GBP/USD would
 * contain the rates for GBP to USD.
 * <P>
 * This example maintains a local map of the rates and also notifies a listener
 * of any rates changes.
 *
 * @author Push Technology Limited
 * @since 6.0
 * @see ControlClientUpdatingRecordV2Topics
 */
public final class ClientConsumingRecordV2Topics {

    private static final Logger LOG =
        LoggerFactory.getLogger(ClientConsumingRecordV2Topics.class);

    private static final String ROOT_TOPIC = "FX";

    /**
     * The map of currency codes to currency objects which each maintain rates
     * for each target currency.
     */
    private final Map<String, Currency> currencies = new ConcurrentHashMap<>();

    private Schema schema;

    private final RatesListener listener;

    private final Session session;

    /**
     * Constructor.
     *
     * @param serverUrl for example "ws://diffusion.example.com:80"
     * @param listener a listener that will be notified of all rates and rate
     *        changes
     */
    public ClientConsumingRecordV2Topics(String serverUrl,
        RatesListener listener) {

        this.listener = requireNonNull(listener);

        session =
            Diffusion.sessions().principal("client").password("password")
                .open(serverUrl);

        // Use the Topics feature to add a record value stream and subscribe to
        // all topics under the root.
        final Topics topics = session.feature(Topics.class);
        final String topicSelector = String.format("?%s//", ROOT_TOPIC);

        topics.addStream(
            topicSelector,
            RecordV2.class,
            new RatesValueStream());

        topics.subscribe(topicSelector)
            .whenComplete((voidResult, exception) -> {
                if (exception != null) {
                    LOG.info("subscription failed", exception);
                }
            });
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
     * This is used to apply topic stream updates to the local map and notify
     * listener of changes.
     */
    private void applyUpdate(
        String currency,
        String targetCurrency,
        RecordV2 oldValue,
        RecordV2 newValue) {

        Currency currencyObject = currencies.get(currency);
        if (currencyObject == null) {
            currencyObject = new Currency();
            currencies.put(currency, currencyObject);
        }

        if (schema == null) {
            updateWithoutSchema(
                currency,
                targetCurrency,
                oldValue,
                newValue,
                currencyObject);
        }
        else {
            updateWithSchema(
                currency,
                targetCurrency,
                oldValue,
                newValue,
                currencyObject);
        }
    }

    private void updateWithSchema(
        String currency,
        String targetCurrency,
        RecordV2 oldValue,
        RecordV2 newValue,
        Currency currencyObject) {

        // A data model is generated using the schema allowing direct access to
        // the fields within it
        final RecordModel model = newValue.asModel(schema);
        final String bid = model.get("Bid");
        final String ask = model.get("Ask");

        currencyObject.setRate(targetCurrency, bid, ask);

        if (oldValue == null) {
            listener.onNewRate(currency, targetCurrency, bid, ask);
        }
        else {
            // Generate a structural delta to determine what has changed
            final RecordV2Delta delta = newValue.diff(oldValue);
            for (Change change : delta.changes(schema)) {
                final String fieldName = change.fieldName();
                listener.onRateChange(
                    currency,
                    targetCurrency,
                    fieldName,
                    model.get(fieldName));
            }
        }
    }

    private void updateWithoutSchema(
        String currency,
        String targetCurrency,
        RecordV2 oldValue,
        RecordV2 newValue,
        Currency currencyObject) {

        // All of the fields in the value are obtained.
        final List<String> fields = newValue.asFields();
        final String bid = fields.get(0);
        final String ask = fields.get(1);

        currencyObject.setRate(targetCurrency, bid, ask);

        if (oldValue == null) {
            listener.onNewRate(currency, targetCurrency, bid, ask);
        }
        else {
            // Fields in the old value are obtained to determine what has
            // changed
            final List<String> oldfields = oldValue.asFields();
            final String oldBid = oldfields.get(0);
            final String oldAsk = oldfields.get(1);
            if (!bid.equals(oldBid)) {
                listener.onRateChange(currency, targetCurrency, "Bid", bid);
            }
            if (!ask.equals(oldAsk)) {
                listener.onRateChange(currency, targetCurrency, "Ask", ask);
            }
        }
    }

    private void removeCurrency(String currency) {
        final Currency oldCurrency = currencies.remove(currency);
        for (String targetCurrency : oldCurrency.rates.keySet()) {
            listener.onRateRemoved(currency, targetCurrency);
        }
    }

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

        private void setRate(String currency, String bid, String ask) {
            rates.put(currency, new Rates(bid, ask));
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
         * @param bid rate
         * @param ask rate
         */
        void onNewRate(String currency, String targetCurrency, String bid,
            String ask);

        /**
         * Notification of a change to the bid or ask value for a rate.
         *
         * @param currency the base currency
         * @param targetCurrency the target currency
         * @param bidOrAsk "Bid" or "Ask"
         * @param rate the new rate
         */
        void onRateChange(String currency, String targetCurrency,
            String bidOrAsk, String rate);

        /**
         * Notification of a rate being removed.
         *
         * @param currency the base currency
         * @param targetCurrency the target currency
         */
        void onRateRemoved(String currency, String targetCurrency);
    }

    private final class RatesValueStream
        extends ValueStream.Default<RecordV2> {

        @Override
        public void onSubscription(String topicPath, TopicSpecification specification) {
            final boolean isRatesTopic = Diffusion.topicSelectors().parse("?FX/.*/.*").selects(topicPath);
            // Only retrieve a schema when subscribing to a rates topic
            if (isRatesTopic) {
                final String schemaString = specification.getProperties().get(TopicSpecification.SCHEMA);
                // If a schema is provided on subscription, retrieve it and set it once
                // All schemas are identical for rates topics.
                if (schemaString != null && schema == null) {
                    try {
                        schema = Diffusion.dataTypes().recordV2().parseSchema(schemaString);
                    }
                    catch (SchemaParseException e) {
                        LOG.error("Unable to parse recordV2 schema", e);
                    }
                }
            }
        }

        @Override
        public void onValue(String topicPath, TopicSpecification specification,
            RecordV2 oldValue, RecordV2 newValue) {
            final String[] topicElements = elements(topicPath);
            // It is only a rate update if topic has 2 elements below root path
            if (topicElements.length == 2) {
                applyUpdate(
                    topicElements[0], // The base currency
                    topicElements[1], // The target currency
                    oldValue,
                    newValue);
            }
        }

        @Override
        public void onUnsubscription(String topicPath,
            TopicSpecification specification, UnsubscribeReason reason) {
            final String[] topicElements = elements(topicPath);
            if (topicElements.length == 2) {
                removeRate(topicElements[0], topicElements[1]);
            }
            else if (topicElements.length == 1) {
                removeCurrency(topicElements[0]);
            }
        }

        private String[] elements(String topicPath) {
            final String subPath =
                topicPath.replaceFirst("^" + ROOT_TOPIC + "/", "");
            return subPath.split("/");
        }
    }

}
