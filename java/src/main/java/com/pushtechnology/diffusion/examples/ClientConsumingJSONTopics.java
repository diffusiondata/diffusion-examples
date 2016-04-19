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

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;
import com.fasterxml.jackson.dataformat.cbor.CBORParser;
import com.pushtechnology.diffusion.client.Diffusion;
import com.pushtechnology.diffusion.client.features.Topics;
import com.pushtechnology.diffusion.client.features.Topics.UnsubscribeReason;
import com.pushtechnology.diffusion.client.session.Session;
import com.pushtechnology.diffusion.client.topics.details.TopicSpecification;
import com.pushtechnology.diffusion.datatype.json.JSON;

/**
 * This demonstrates a client consuming JSON topics.
 * <P>
 * It is assumed that under the FX topic there is a JSON topic for each currency
 * which contains a map of conversion rates to each target currency. For
 * example, FX/GBP could contain {"USD":"123.45","HKD":"456.3"}.
 * <P>
 * All updates will be notified to a listener.
 *
 * @author Push Technology Limited
 * @since 5.7
 * @see ControlClientUpdatingJSONTopics
 */
public final class ClientConsumingJSONTopics {

    private static final String ROOT_TOPIC = "FX";

    private final RatesListener listener;

    private final Session session;

    /**
     * Constructor.
     *
     * @param serverUrl for example "ws://diffusion.example.com:80
     */
    public ClientConsumingJSONTopics(String serverUrl, RatesListener listener) {

        this.listener = requireNonNull(listener);

        session =
            Diffusion.sessions().principal("client").password("password")
                .open(serverUrl);

        // Use the Topics feature to add a topic stream and subscribe to all
        // topics under the root
        final Topics topics = session.feature(Topics.class);
        final String topicSelector = String.format("?%s/", ROOT_TOPIC);
        topics.addStream(topicSelector, JSON.class, new RatesStream());
        topics
            .subscribe(topicSelector, new Topics.CompletionCallback.Default());
    }

    /**
     * Close session.
     */
    public void close() {
        session.close();
    }

    private static String pathToCurrency(String path) {
        return path.substring(path.indexOf('/') + 1);
    }

    /**
     * A listener for Rates updates.
     */
    public interface RatesListener {

        /**
         * Notification of a new rate or rate update.
         *
         * @param currency the base currency
         * @param rates map of rates
         */
        void onNewRates(String currency, Map<String, BigDecimal> rates);

        /**
         * Notification of a rate being removed.
         *
         * @param currency the base currency
         */
        void onRatesRemoved(String currency);
    }

    /**
     * The value stream.
     */
    private final class RatesStream extends Topics.ValueStream.Default<JSON> {

        private final CBORFactory factory = new CBORFactory();
        private final ObjectMapper mapper = new ObjectMapper();
        private final TypeReference<Map<String, BigDecimal>> typeReference =
            new TypeReference<Map<String, BigDecimal>>() {
            };

        @Override
        public void onValue(
            String topicPath,
            TopicSpecification specification,
            JSON oldValue,
            JSON newValue) {
            try {
                // Use the third-party Jackson library to parse the newValue's
                // binary representation and convert to a map
                final CBORParser parser =
                    factory.createParser(newValue.asInputStream());
                final Map<String, BigDecimal> map =
                    mapper.readValue(parser, typeReference);
                final String currency = pathToCurrency(topicPath);
                listener.onNewRates(currency, map);
            }
            catch (IOException ex) {
                ex.printStackTrace();
            }
        }

        @Override
        public void onUnsubscription(
            String topicPath,
            TopicSpecification specification,
            UnsubscribeReason reason) {

            final String currency = pathToCurrency(topicPath);
            listener.onRatesRemoved(currency);
        }

    }
}
