/*******************************************************************************
 * Copyright (C) 2015, 2020 Push Technology Ltd.
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

import com.pushtechnology.diffusion.client.Diffusion;
import com.pushtechnology.diffusion.client.callbacks.ErrorReason;
import com.pushtechnology.diffusion.client.features.Messaging;
import com.pushtechnology.diffusion.client.features.Messaging.FilteredRequestCallback;
import com.pushtechnology.diffusion.client.features.Messaging.RequestHandler;
import com.pushtechnology.diffusion.client.session.Session;

/**
 * This is an example of a control client using the 'Messaging' feature to send
 * messages to clients using message filters. It also demonstrates the ability
 * to register a request handler with an interest in session property values.
 *
 * @author Push Technology Limited
 * @since 5.5
 */
public final class ControlClientUsingFiltersAndProperties {

    private final Session session;
    private final Messaging messaging;

    private static final FilteredRequestCallback<String> FILTERED_CALLBACK =
        new FilteredRequestCallback.Default<>();

    /**
     * Constructor.
     * @throws TimeoutException
     * @throws ExecutionException
     * @throws InterruptedException
     */
    public ControlClientUsingFiltersAndProperties(String serverUrl)
        throws InterruptedException, ExecutionException, TimeoutException {

        session =
            Diffusion.sessions().principal("control").password("password")
                .open(serverUrl);

        messaging = session.feature(Messaging.class);

        // Register to receive all messages sent by clients on the "foo" branch
        // and include the "JobTitle" session property value with each message.
        // To do this, the client session must have the 'register_handler'
        // permission.
        messaging.addRequestHandler(
            "foo",
            String.class,
            String.class,
            new BroadcastHandler(),
            "JobTitle").get(5, TimeUnit.SECONDS);
    }

    /**
     * Close the session.
     */
    public void close() {
        session.close();
    }

    /**
     * Handler that will pass any message to all sessions that have a "JobTitle"
     * property set to "Staff" if, and only if it comes from a session that has
     * a "JobTitle" set to "Manager".
     */
    private class BroadcastHandler implements RequestHandler<String, String> {

        @Override
        public void onRequest(
            String request,
            RequestContext context,
            final Responder<String> responder) {

            if ("Manager".equals(
                context.getSessionProperties().get("JobTitle"))) {
                messaging.sendRequestToFilter(
                    "JobTitle is 'Staff'",
                    "foo",
                    request,
                    String.class,
                    String.class,
                    FILTERED_CALLBACK)
                    .thenAccept(sentTo ->
                        responder.respond("Sent to " + sentTo + " staff"));
            }
            else {
                responder.respond("Not sent");
            }
        }

        @Override
        public void onClose() {
        }

        @Override
        public void onError(ErrorReason errorReason) {
        }
    }

}
