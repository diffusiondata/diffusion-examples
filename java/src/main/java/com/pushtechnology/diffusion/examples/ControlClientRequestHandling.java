/*******************************************************************************
 * Copyright (C) 2017, 2020 Push Technology Ltd.
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
import com.pushtechnology.diffusion.client.callbacks.Registration;
import com.pushtechnology.diffusion.client.features.Messaging;
import com.pushtechnology.diffusion.client.session.Session;
import com.pushtechnology.diffusion.client.session.SessionId;
import com.pushtechnology.diffusion.datatype.json.JSON;

/**
 * This is an example of a control client using the 'Messaging' feature
 * to receive requests from clients and send requests to clients.
 * <P>
 * It is a trivial example that simply responds to all requests on a particular
 * branch of the topic tree by echoing them back to the client exactly as they
 * are.
 *
 * @see ClientRequestHandling
 *
 * @author Push Technology Limited
 * @since 6.0
 */
public class ControlClientRequestHandling {

    private static final Logger LOG =
        LoggerFactory.getLogger(ControlClientRequestHandling.class);

    private final Session session;
    private final Messaging messaging;

    /**
     * Constructor.
     *
     * @param serverURL url of the server to connect to.
     */
    public ControlClientRequestHandling(String serverURL) {
        session = Diffusion.sessions().principal("control").password("password")
            .open(serverURL);
        messaging = session.feature(Messaging.class);
    }

    /**
     * Register a request handler on a path.
     *
     * @param messagePath path to register a handler on
     * @return handler registration
     */
    public Registration addRequestHandler(String messagePath)
        throws InterruptedException, ExecutionException, TimeoutException {
        return messaging.addRequestHandler(
            messagePath, JSON.class, JSON.class, new JSONRequestHandler()).get(5, TimeUnit.SECONDS);
    }

    /**
     * Sends a request directly to the request echoing session.
     *
     * @param sessionId session to send a request to
     * @param messagePath path to send the request to
     * @param request request to send
     *
     * @return the response to the request
     */
    public JSON sendRequest(SessionId sessionId, String messagePath, JSON request)
        throws InterruptedException, ExecutionException, TimeoutException {
        final JSON response = messaging.sendRequest(
                sessionId, messagePath, request, JSON.class, JSON.class).get(5, TimeUnit.SECONDS);
        LOG.info("Response received: {}", response.toJsonString());

        return response;
    }

    /**
     * Close the session.
     */
    public void close() {
        session.close();
    }

    /**
     * Send a request to sessions using a filter to match against receiving sessions.
     *
     * @param messagePath path to send the request to
     * @param request the request to send
     * @param filter filter to match sessions against
     *
     * @return the number of requests dispatched to clients
     */
    public int sendRequestToFilter(String messagePath, JSON request, String filter)
        throws InterruptedException, ExecutionException, TimeoutException {
        final int numberSent = messaging.sendRequestToFilter(filter, messagePath, request,
                JSON.class, JSON.class, new JSONFilterRequestCallback()).get(5, TimeUnit.SECONDS);
        LOG.info("{} requests sent", numberSent);

        return numberSent;
    }

    /**
     * Request handler that logs received requests and echoes them back to the original session.
     */
    private final class JSONRequestHandler implements Messaging.RequestHandler<JSON, JSON> {

        @Override
        public void onClose() {
            LOG.info("JSONRequestHandler closed");
        }

        @Override
        public void onError(ErrorReason errorReason) {
            LOG.info("JSONRequestHandler error: {}", errorReason);
        }

        @Override
        public void onRequest(JSON request, RequestContext context, Responder<JSON> responder) {
            LOG.info("Handler received request: {} from session {}", request.toJsonString(), context.getSessionId());
            //Echo the request back to the requester
            responder.respond(request);
        }
    }

    /**
     * Filter callback that logs responses to the request.
     */
    private final class JSONFilterRequestCallback implements Messaging.FilteredRequestCallback<JSON> {

        @Override
        public void onClose() {
            // This method was deprecated in 6.5 and is no longer called. It
            // will be removed in a future release.
        }

        @Override
        public void onError(ErrorReason errorReason) {
            // This method was deprecated in 6.5 and is no longer called. It
            // will be removed in a future release.
        }

        @Override
        public void onResponse(SessionId sessionId, JSON response) {
            LOG.info("Response received: {} from session {}", response.toJsonString(), sessionId);
        }

        @Override
        public void onResponseError(SessionId sessionId, Throwable throwable) {
            LOG.info("Response error from session {} due to {}", sessionId, throwable);
        }
    }
}
