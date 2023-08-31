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

package com.pushtechnology.diffusion.examples.runnable;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;
import com.fasterxml.jackson.dataformat.cbor.CBORParser;
import com.pushtechnology.diffusion.client.features.Topics;
import com.pushtechnology.diffusion.client.session.Session;
import com.pushtechnology.diffusion.client.topics.details.TopicSpecification;
import com.pushtechnology.diffusion.datatype.json.JSON;

/**
 * A client that consumes JSON topics.
 *
 * @author DiffusionData Limited
 * @since 5.7
 */
public final class ConsumingJson extends AbstractClient {
    private static final CBORFactory CBOR_FACTORY = new CBORFactory();
    private static final ObjectMapper OBJECT_MAPPER =
        new ObjectMapper(CBOR_FACTORY);
    private static final Logger LOG =
        LoggerFactory.getLogger(ConsumingJson.class);
    private static final TypeReference<Map<String, BigInteger>> INT_MAP_TYPE =
        new TypeReference<Map<String, BigInteger>>() { };

    /**
     * Constructor.
     * @param url The URL to connect to
     * @param principal The principal to connect as
     */
    public ConsumingJson(String url, String principal) {
        super(url, principal);
    }

    @Override
    public void onStarted(Session session) {
        final Topics topics = session.feature(Topics.class);

        // Add the stream to receive JSON values
        topics.addStream(
            ">json/random",
            JSON.class,
            new Topics.ValueStream.Default<JSON>() {
                @Override
                public void onValue(
                    String topicPath,
                    TopicSpecification specification,
                    JSON oldValue,
                    JSON newValue) {

                    try {
                        // Converts a Json map into a Java Map.
                        final CBORParser parser =
                            CBOR_FACTORY.createParser(newValue.asInputStream());
                        final Map<String, BigInteger> map = OBJECT_MAPPER
                            .readValue(parser, INT_MAP_TYPE);
                        parser.close();

                        // Log the timestamp from the map
                        LOG.info("New timestamp {}", map.get("timestamp"));
                    }
                    catch (IOException e) {
                        LOG.warn("Failed to transform value '{}'", newValue, e);
                    }
                }
            });

        // Subscribe to the topic
        topics.subscribe("json/random")
            .whenComplete((voidResult, exception) -> {
                if (exception != null) {
                    LOG.info("subscription failed", exception);
                }
            });
    }

    /**
     * Entry point for the example.
     * @param args The command line arguments
     * @throws InterruptedException If the main thread was interrupted
     */
    public static void main(String[] args) throws InterruptedException {
        final ConsumingJson client =
            new ConsumingJson("ws://diffusion.example.com:80", "auth");
        client.start("auth_secret");
        client.waitForStopped();
    }
}
