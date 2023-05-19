/*******************************************************************************
 * Copyright (C) 2014, 2020 Push Technology Ltd.
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pushtechnology.diffusion.client.Diffusion;
import com.pushtechnology.diffusion.client.callbacks.Registration;
import com.pushtechnology.diffusion.client.features.control.topics.SubscriptionControl;
import com.pushtechnology.diffusion.client.features.control.topics.SubscriptionControl.RoutingSubscriptionRequest;
import com.pushtechnology.diffusion.client.features.control.topics.SubscriptionControl.RoutingSubscriptionRequest.RoutingHandler;
import com.pushtechnology.diffusion.client.session.Session;

/**
 * This demonstrates using a control client to be notified of subscription
 * requests to routing topics.
 * <P>
 * This uses the 'SubscriptionControl' feature.
 *
 * @author DiffusionData Limited
 * @since 5.0
 */
public class ControlClientSubscriptionControlRouting {

    private static final Logger LOG =
        LoggerFactory.getLogger(ControlClientSubscriptionControlRouting.class);

    private final Session session;
    private final SubscriptionControl subscriptionControl;

    /**
     * Constructor.
     */
    public ControlClientSubscriptionControlRouting(String serverUrl) {

        session =
            Diffusion.sessions().principal("control").password("password")
                .open(serverUrl);

        subscriptionControl =
            session.feature(SubscriptionControl.class);

    }

    /**
     * Route a subscription from one topic to another.
     *
     * @param topic the topic to receive data from its routed subscription
     * @param sourceTopic the topic to route subscriptions to
     * @return a CompletableFuture that completes when a response is received
     *         from the server
     */
    public CompletableFuture<Registration> addRoute(String topic, String sourceTopic) {
        // Sets up a handler so that all subscriptions to topic are routed
        // to sourceTopic
        // To do this, the client session requires the 'view_session',
        // 'modify_session', and 'register_handler' permissions.
        return subscriptionControl.addRoutingSubscriptionHandler(
            topic,
            new RoutingHandler.Default() {
                @Override
                public void onSubscriptionRequest(
                    final RoutingSubscriptionRequest request) {

                    request.route(sourceTopic).whenComplete((voidResult, exception) -> {
                            if (exception != null) {
                                LOG.info("subscription routing failed", exception);
                            }
                    });
                }
            });
    }

    /**
     * Close the session.
     */
    public void close() {
        session.close();
    }
}
