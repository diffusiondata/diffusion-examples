/*******************************************************************************
 * Copyright (C) 2015, 2018 Push Technology Ltd.
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

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import com.pushtechnology.diffusion.client.Diffusion;
import com.pushtechnology.diffusion.client.features.control.clients.ClientControl;
import com.pushtechnology.diffusion.client.features.control.topics.SubscriptionControl;
import com.pushtechnology.diffusion.client.features.control.topics.SubscriptionControl.SubscriptionByFilterResult;
import com.pushtechnology.diffusion.client.session.Session;
import com.pushtechnology.diffusion.client.session.SessionId;

/**
 * This is an example of a control client using both the 'ClientControl' feature
 * and the 'SubscriptionControl' feature to monitor and subscribe clients.
 * <P>
 * The example shows a control client that configures all clients that are in
 * Italy and are in the "Accounts" department (determined by an additional
 * property) to be subscribed to the "ITAccounts" topic.
 * <P>
 * It also has a method which makes use of filtered subscription to change the
 * topic that all matching clients are subscribed to.
 *
 * @author Push Technology Limited
 * @since 5.6
 */
public final class ControlClientUsingSessionProperties {

    private final Session session;
    private final ClientControl clientControl;
    private final SubscriptionControl subscriptionControl;

    private volatile String currentTopic = "ITAccounts";

    /**
     * Constructor.
     */
    public ControlClientUsingSessionProperties() {

        session =
            Diffusion.sessions().principal("control").password("password")
                .open("ws://diffusion.example.com:80");

        clientControl = session.feature(ClientControl.class);
        subscriptionControl = session.feature(SubscriptionControl.class);

        /**
         * Configure a listener will be notified firstly of all open client
         * sessions and then of all that subsequently open. All that are in the
         * Italian Accounts department get subscribed to the current topic.
         * The country and department properties only are requested.
         */
        clientControl.setSessionPropertiesListener(
            new ClientControl.SessionPropertiesListener.Default() {
                @Override
                public void onSessionOpen(
                    SessionId sessionId,
                    Map<String, String> properties) {

                    if ("Accounts".equals(properties.get("Department")) &&
                        "IT".equals(properties.get("$Country"))) {

                        subscriptionControl.subscribe(
                            sessionId,
                            currentTopic);
                    }
                }
            },
            "$Country", "$Department");
    }

    /**
     * This can be used to change the topic that all of the Italian accounts
     * department is subscribed to. It will unsubscribe all current clients from
     * the old topic and subscribe them to the new one. All new clients will be
     * subscribed to the new one.
     *
     * @param newTopic the new topic name
     * @return a CompletableFuture that completes when the operation succeeds or
     *         fails.
     *
     *         <p>
     *         If the operation was successful, the CompletableFuture will
     *         complete successfully.
     *
     *         <p>
     *         Otherwise, the CompletableFuture will complete exceptionally with
     *         an {@link ExecutionException}. See
     *         {@link SubscriptionControl#unsubscribeByFilter} and
     *         {@link SubscriptionControl#subscribeByFilter} for common failure
     *         reasons.
     */
    public CompletableFuture<SubscriptionByFilterResult>
        changeTopic(String newTopic) {

        final String filter = "Department is 'Accounts' and $Country is 'IT'";

        final String oldTopic = currentTopic;
        // Change the topic that all new clients will be subscribed to.
        currentTopic = newTopic;

        return
            // Unsubscribe all from the current topic...
            subscriptionControl.unsubscribeByFilter(filter, oldTopic)

            // ...then subscribe all to the new topic...
            .thenCompose(unsubscriptionResult ->
                subscriptionControl.subscribeByFilter(filter, currentTopic));
    }

    /**
     * Close the session.
     */
    public void close() {
        session.close();
    }

}
