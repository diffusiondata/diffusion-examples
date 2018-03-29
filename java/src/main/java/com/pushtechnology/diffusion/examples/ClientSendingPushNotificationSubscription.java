/*******************************************************************************
 * Copyright (C) 2017 Push Technology Ltd.
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
import java.util.concurrent.ExecutionException;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pushtechnology.diffusion.client.Diffusion;
import com.pushtechnology.diffusion.client.features.Messaging;
import com.pushtechnology.diffusion.client.session.Session;
import com.pushtechnology.diffusion.datatype.json.JSON;

/**
 * An example of a client using the 'Messaging' feature to request the Push
 * Notification Bridge subscribe to a topic and relay updates to a GCM
 * registration ID.
 *
 * @author Push Technology Limited
 * @since 5.9
 */
public class ClientSendingPushNotificationSubscription {

    private static final Logger LOG = LoggerFactory
        .getLogger(ClientSendingPushNotificationSubscription.class);

    private final String pushServiceTopicPath;
    private final Session session;
    private final Messaging messaging;

    /**
     * Constructs message sending application.
     *
     * @param pushServiceTopicPath topic path on which the Push Notification
     *        Bridge is taking requests.
     */
    public ClientSendingPushNotificationSubscription(
        String pushServiceTopicPath) {
        this.pushServiceTopicPath = pushServiceTopicPath;
        this.session =
            Diffusion.sessions().principal("client")
                .password("password").open("ws://diffusion.example.com:80");
        this.messaging = session.feature(Messaging.class);
    }

    /**
     * Close the session.
     */
    public void close() {
        session.close();
    }

    /**
     * Compose & send a subscription request to the Push Notification Bridge.
     *
     * @param subscribedTopic topic to which the bridge subscribes.
     * @param gcmRegistrationID GCM registration ID to which the bridge relays
     *        updates.
     * @throws ExecutionException If the Push Notification Bridge cannot process
     *         the request
     * @throws InterruptedException If the current thread was interrupted while
     *         waiting for a response
     */
    public void requestPNSubscription(String gcmRegistrationID,
        String subscribedTopic)
        throws InterruptedException, ExecutionException {

        // Compose the request
        final String gcmDestination = "gcm://" + gcmRegistrationID;
        final JSONObject jsonObject =
            buildSubscriptionRequest(gcmDestination, subscribedTopic);
        final JSON request =
            Diffusion.dataTypes().json().fromJsonString(jsonObject.toString());

        // Send the request
        final CompletableFuture<JSON> response =
            messaging.sendRequest(
                pushServiceTopicPath,
                request,
                JSON.class,
                JSON.class);

        LOG.info("Received response from PN Bridge: {}",
            response.get().toJsonString());
    }

    /**
     * Compose a subscription request.
     * <P>
     *
     * @param destination The {@code gcm://} or {@code apns://} destination for
     *        any push notifications.
     * @param topic Diffusion topic subscribed-to by the Push Notification
     *        Bridge.
     * @return a complete request
     */
    private static JSONObject buildSubscriptionRequest(
        String destination,
        String topic) {

        final JSONObject subObject = new JSONObject();

        subObject
            .put("destination", destination)
            .put("topic", topic);

        final JSONObject contentObj = new JSONObject();
        contentObj.put("pnsub", subObject);

        return contentObj;
    }
}
