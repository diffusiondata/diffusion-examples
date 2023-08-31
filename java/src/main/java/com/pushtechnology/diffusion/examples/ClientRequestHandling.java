/*******************************************************************************
 * Copyright (C) 2017, 2023 DiffusionData Ltd.
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pushtechnology.diffusion.client.Diffusion;
import com.pushtechnology.diffusion.client.callbacks.ErrorReason;
import com.pushtechnology.diffusion.client.features.Messaging;
import com.pushtechnology.diffusion.client.features.Messaging.RequestStream;
import com.pushtechnology.diffusion.client.session.Session;
import com.pushtechnology.diffusion.client.session.SessionId;
import com.pushtechnology.diffusion.datatype.json.JSON;

/**
 * This is a simple example of a client that uses the request/response
 * API of the 'Messaging' feature to send requests to a topic path, and
 * receive requests sent directly to the session.
 * <P>
 * To send a request on a topic path, the client session requires the
 * 'send_to_message_handler' permission.
 *
 * @see ControlClientRequestHandling
 *
 * @author DiffusionData Limited
 * @since 6.0
 */
public class ClientRequestHandling {

    private static final Logger LOG =
        LoggerFactory.getLogger(ClientRequestHandling.class);

    private final Session session;
    private final Messaging messaging;

    /**
     * Construct a request handling application.
     *
     * @param serverURL url of the server to connect to.
     */
    public ClientRequestHandling(String serverURL) {
        session = Diffusion.sessions().principal("client").password("password")
            .open(serverURL);
        messaging = session.feature(Messaging.class);
    }

    /**
     * Get the sessionId of the session.
     *
     * @return the session id of the session
     */
    public SessionId getSessionId() {
        return session.getSessionId();
    }

    /**
     * Sets up a JSON {@link RequestStream} for a particular message path.
     *
     * @param messagePath message path to receive requests from
     */
    public void setRequestStream(String messagePath) {
        messaging.setRequestStream(messagePath, JSON.class, JSON.class, new JSONRequestStream());
    }

    /**
     * Sends a simple JSON request to a specified message path.
     * The response is logged when received.
     *
     * @param messagePath the message path
     * @param request JSON request to send
     *
     * @return response to the request
     */
    public JSON send(String messagePath, JSON request)
        throws InterruptedException, ExecutionException, TimeoutException {
        final JSON response =
            messaging.sendRequest(messagePath, request, JSON.class, JSON.class).get(10, TimeUnit.SECONDS);
        LOG.info("Response received: {}", response);

        return response;
    }

    /**
     * Close the session.
     */
    public void close() {
        session.close();
    }

    /**
     * Request stream that logs received requests and echoes them back to the original client.
     */
    private final class JSONRequestStream implements RequestStream<JSON, JSON> {

        @Override
        public void onClose() {
            LOG.info("JSONRequestStream closed");
        }

        @Override
        public void onError(ErrorReason errorReason) {
            LOG.info("JSONRequestStream error: {}", errorReason);
        }

        @Override
        public void onRequest(String path, JSON request, Responder<JSON> responder) {
            LOG.info("Stream received request: {} on message path: {}", request.toJsonString(), path);
            //Echo the request back to the requester
            responder.respond(request);
        }
    }
}
