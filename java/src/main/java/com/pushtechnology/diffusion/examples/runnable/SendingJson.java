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

package com.pushtechnology.diffusion.examples.runnable;

import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.pushtechnology.diffusion.client.Diffusion;
import com.pushtechnology.diffusion.client.content.Content;
import com.pushtechnology.diffusion.client.features.Messaging;
import com.pushtechnology.diffusion.client.features.Messaging.MessageStream;
import com.pushtechnology.diffusion.client.session.Session;
import com.pushtechnology.diffusion.client.types.ReceiveContext;
import com.pushtechnology.diffusion.datatype.json.JSON;

/**
 * A client that creates and sends JSON messages.
 *
 * @author Push Technology Limited
 * @since 5.7
 */
public final class SendingJson extends AbstractClient {
    private static final Logger LOG = LoggerFactory
        .getLogger(ProducingJson.class);
    private static final Messaging.SendCallback.Default SEND_CALLBACK =
        new Messaging.SendCallback.Default();
    private final ScheduledExecutorService executor = Executors
        .newSingleThreadScheduledExecutor();

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
        final Messaging messagingFeature = session.feature(Messaging.class);

        // Add a message handler for receiving messages sent to the session
        messagingFeature.addMessageStream(
            ">json/response",
            new MessageStream.Default() {
                @Override
                public void onMessageReceived(
                    String topicPath,
                    Content content,
                    ReceiveContext context) {

                    // Convert the content to JSON
                    final JSON json = Diffusion
                        .dataTypes()
                        .json()
                        .readValue(content);

                    LOG.info("Received response {}", json);

                    executor.schedule(
                        new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    // Send the next message to the server
                                    messagingFeature.send(
                                        "json/request",
                                        RandomData.toJSON(RandomData.next()),
                                        SEND_CALLBACK);
                                }
                                catch (JsonProcessingException e) {
                                    LOG.error("Failed to transform RandomData" +
                                        " to Content");
                                }
                            }
                        },
                        1,
                        SECONDS);
                }
        });

        // Send the first message to the server
        try {
            messagingFeature.send(
                "json/request",
                RandomData.toJSON(RandomData.next()),
                SEND_CALLBACK);
        }
        catch (JsonProcessingException e) {
            LOG.error("Failed to transform RandomData to Content");
        }
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
