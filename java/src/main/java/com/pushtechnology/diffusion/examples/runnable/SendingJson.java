/*******************************************************************************
 * Copyright (C) 2016, 2018 Push Technology Ltd.
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

import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.pushtechnology.diffusion.client.features.Messaging;
import com.pushtechnology.diffusion.client.session.Session;
import com.pushtechnology.diffusion.datatype.json.JSON;

/**
 * A client that creates and sends JSON requests.
 *
 * @author DiffusionData Limited
 * @since 5.7
 * @see ReceivingJson
 */
public final class SendingJson extends AbstractClient {
    private static final Logger LOG = LoggerFactory
        .getLogger(ProducingJson.class);

    /**
     * Constructor.
     * @param url The URL to connect to
     * @param principal The principal to connect as
     */
    public SendingJson(String url, String principal) {
        super(url, principal);
    }

    @Override
    public void onConnected(Session session) {

        final JSON request;

        try {
            request = RandomData.toJSON(RandomData.next());
        }
        catch (JsonProcessingException e) {
            LOG.error("Failed to transform RandomData to Content");
            return;
        }

        final Messaging messaging = session.feature(Messaging.class);

        // Send the request to the server
        final CompletableFuture<JSON> response =
            messaging.sendRequest(
            "json/request",
            request,
            JSON.class,
            JSON.class);

        response.whenComplete((result, error) -> {
            if (error != null) {
                LOG.error("Request failed", error);
            }
            else {
                LOG.info("Received response {}", result);
            }
        });
    }

    /**
     * Entry point for the example.
     * @param args The command line arguments
     * @throws InterruptedException If the main thread was interrupted
     */
    public static void main(String[] args) throws InterruptedException {
        final SendingJson client =
            new SendingJson("ws://diffusion.example.com:80", "auth");
        client.start("auth_secret");
        client.waitForStopped();
    }
}
