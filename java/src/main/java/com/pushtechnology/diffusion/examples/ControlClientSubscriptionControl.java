/*******************************************************************************
 * Copyright (C) 2014, 2018 Push Technology Ltd.
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

import java.util.concurrent.CompletableFuture;

import com.pushtechnology.diffusion.client.Diffusion;
import com.pushtechnology.diffusion.client.features.control.topics.SubscriptionControl;
import com.pushtechnology.diffusion.client.session.Session;
import com.pushtechnology.diffusion.client.session.SessionId;

/**
 * This demonstrates using a client to subscribe and unsubscribe other clients
 * to topics.
 * <P>
 * This uses the 'SubscriptionControl' feature.
 *
 * @author DiffusionData Limited
 * @since 5.0
 */
public class ControlClientSubscriptionControl {

    private final Session session;

    private final SubscriptionControl subscriptionControl;

    /**
     * Constructor.
     */
    public ControlClientSubscriptionControl(String serverUrl) {

        session =
            Diffusion.sessions().principal("control").password("password")
                .open(serverUrl);

        subscriptionControl = session.feature(SubscriptionControl.class);
    }

    /**
     * Subscribe a client to topics.
     *
     * @param sessionId client to subscribe
     * @param topicSelector topic selector expression
     * @return a CompletableFuture that completes when a response is received
     *         from the server
     */
    public CompletableFuture<?> subscribe(
        SessionId sessionId,
        String topicSelector) {

        // To subscribe a client to a topic, this client session
        // must have the 'modify_session' permission.
        return subscriptionControl.subscribe(
            sessionId,
            topicSelector);
    }

    /**
     * Unsubscribe a client from topics.
     *
     * @param sessionId client to unsubscribe
     * @param topicSelector topic selector expression
     * @return a CompletableFuture that completes when a response is received
     *         from the server
     */
    public CompletableFuture<?> unsubscribe(
        SessionId sessionId,
        String topicSelector) {

        // To unsubscribe a client from a topic, this client session
        // must have the 'modify_session' permission.
        return subscriptionControl.unsubscribe(
            sessionId,
            topicSelector);
    }

    /**
     * Close the session.
     */
    public void close() {
        session.close();
    }
}
