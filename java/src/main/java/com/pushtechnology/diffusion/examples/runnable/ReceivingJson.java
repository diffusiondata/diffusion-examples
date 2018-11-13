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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pushtechnology.diffusion.client.Diffusion;
import com.pushtechnology.diffusion.client.callbacks.ErrorReason;
import com.pushtechnology.diffusion.client.features.control.topics.MessagingControl;
import com.pushtechnology.diffusion.client.features.control.topics.MessagingControl.RequestHandler;
import com.pushtechnology.diffusion.client.session.Session;
import com.pushtechnology.diffusion.datatype.json.JSON;

/**
 * A control client that responds to JSON requests send by the
 * {@link SendingJson} client.
 *
 * @author Push Technology Limited
 * @since 5.7
 */
public final class ReceivingJson extends AbstractClient {
    private static final Logger LOG = LoggerFactory.
        getLogger(ConsumingJson.class);

    /**
     * Constructor.
     * @param url The URL to connect to
     * @param principal The principal to connect as
     */
    public ReceivingJson(String url, String principal) {
        super(url, principal);
    }

    @Override
    public void onConnected(Session session) {
        final MessagingControl messagingControl =
            session.feature(MessagingControl.class);

        // Add a request handler for receiving messages sent to the server
        messagingControl.addRequestHandler(
            "json",
            JSON.class,
            JSON.class,
            new RequestHandler<JSON, JSON>() {
                @Override
                public void onRequest(JSON request, RequestContext context,
                    Responder<JSON> responder) {

                    LOG.info("Received request {}", request);

                    // Respond to the client that sent a message
                    responder.respond(
                        // Create the JSON value 'true'
                        Diffusion.dataTypes().json().fromJsonString("true"));
                }

                @Override
                public void onClose() {
                    LOG.info("RequestHandler closed");
                }

                @Override
                public void onError(ErrorReason errorReason) {
                    LOG.info("RequestHandler error: {}", errorReason);
                }
            });
    }

    /**
     * Entry point for the example.
     * @param args The command line arguments
     * @throws InterruptedException If the main thread was interrupted
     */
    public static void main(String[] args) throws InterruptedException {
        final ReceivingJson client =
            new ReceivingJson("ws://diffusion.example.com:80", "auth");
        client.start("auth_secret");
        client.waitForStopped();
    }
}
