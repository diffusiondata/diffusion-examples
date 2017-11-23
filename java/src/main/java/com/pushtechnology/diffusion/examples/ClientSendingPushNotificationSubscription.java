package com.pushtechnology.diffusion.examples;

import static java.util.UUID.randomUUID;

import org.json.JSONObject;
import org.json.JSONTokener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pushtechnology.diffusion.client.Diffusion;
import com.pushtechnology.diffusion.client.content.Content;
import com.pushtechnology.diffusion.client.features.Messaging;
import com.pushtechnology.diffusion.client.features.Messaging.MessageStream;
import com.pushtechnology.diffusion.client.features.Messaging.SendCallback;
import com.pushtechnology.diffusion.client.session.Session;
import com.pushtechnology.diffusion.client.types.ReceiveContext;

/**
 * An example of a client using the 'Messaging' feature to request the Push Notification Bridge
 * subscribe to a topic and relay updates to a GCM registration ID.
 *
 * @author Push Technology Limited
 * @since 5.9
 */
public class ClientSendingPushNotificationSubscription {

    private static final Logger LOG = LoggerFactory.getLogger(ClientSendingPushNotificationSubscription.class);

    private final String pushServiceTopicPath;
    private final Session session;
    private final Messaging messaging;
    private final MessageStream messageStream = new MessageStream.Default() {
        @Override
        public void onMessageReceived(String topicPath, Content content, ReceiveContext context) {
            final JSONObject response = (JSONObject) new JSONTokener(content.asString()).nextValue();
            final String correlation = response.getJSONObject("response").getString("correlation");

            LOG.info("Received response with correlation {}: {}", correlation, response);
        } };

    /**
     * Constructs message sending application.
     * @param pushServiceTopicPath topic path on which the Push Notification Bridgre is taking requests.
     */
    public ClientSendingPushNotificationSubscription(String pushServiceTopicPath) {
        this.pushServiceTopicPath = pushServiceTopicPath;
        this.session =
            Diffusion.sessions().principal("client").password("password")
                .open("ws://diffusion.example.com:80");
        this.messaging = session.feature(Messaging.class);
         messaging.addMessageStream(pushServiceTopicPath, messageStream);
    }

    /**
     * Close the session.
     */
    public void close() {
        messaging.removeMessageStream(messageStream);
        session.close();
    }

    /**
     * Compose & send a subscription request to the Push Notification Bridge.
     *
     * @param subscribedTopic topic to which the bridge subscribes.
     * @param gcmRegistrationID GCM registration ID to which the bridge relays updates.
     */
    public void requestPNSubscription(String gcmRegistrationID, String subscribedTopic) {
        // Compose the request
        final String gcmDestination = "gcm://" + gcmRegistrationID;
        final String correlation = randomUUID().toString();
        final JSONObject request = buildSubscriptionRequest(gcmDestination, subscribedTopic, correlation);

        // Send the request
        messaging.send(pushServiceTopicPath, request.toString(), new SendCallback.Default());
    }

    /**
     * Compose a subscription request.
     * <P>
     * @param destination The {@code gcm://} or {@code apns://} destination for any push notifications.
     * @param topic Diffusion topic subscribed-to by the Push Notification Bridge.
     * @param correlation value embedded in the response by the bridge relating it back to the request.
     * @return a complete request
     */
    private static JSONObject buildSubscriptionRequest(String destination, String topic, String correlation) {
        final JSONObject subObject = new JSONObject();
        subObject.put("destination", destination);
        subObject.put("topic", topic);

        final JSONObject contentObj = new JSONObject();
        contentObj.put("pnsub", subObject);

        final JSONObject requestObj = new JSONObject();
        requestObj.put("correlation", correlation);
        requestObj.put("content", contentObj);

        final JSONObject rootObject = new JSONObject();
        rootObject.put("request", requestObj);
        return rootObject;
    }
}
