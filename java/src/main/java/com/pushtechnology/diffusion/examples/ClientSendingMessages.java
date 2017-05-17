/*******************************************************************************
 * Copyright (C) 2014, 2017 Push Technology Ltd.
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

import java.util.List;

import com.pushtechnology.diffusion.client.Diffusion;
import com.pushtechnology.diffusion.client.features.Messaging;
import com.pushtechnology.diffusion.client.features.Messaging.SendCallback;
import com.pushtechnology.diffusion.client.features.Messaging.SendContextCallback;
import com.pushtechnology.diffusion.client.session.Session;
import com.pushtechnology.diffusion.datatype.json.JSON;

/**
 * This is a simple example of a client that uses the 'Messaging' feature to
 * send JSON messages to a topic path.
 * <P>
 * To send a message on a topic path, the client session requires the
 * 'send_to_message_handler' permission.
 *
 * @author Push Technology Limited
 * @since 5.0
 * @see ControlClientReceivingMessages
 */
public final class ClientSendingMessages {

    private final Session session;
    private final Messaging messaging;

    /**
     * Constructs a message sending application.
     */
    public ClientSendingMessages() {
        session =
            Diffusion.sessions().principal("client").password("password")
                .open("ws://diffusion.example.com:80");
        messaging = session.feature(Messaging.class);
    }

    /**
     * Sends a JSON message to a specified topic path.
     * <P>
     * A JSON value can be created from a string using:
     * {@code Diffusion.dataTypes().json().fromString(aString)}
     * <P>
     * JSON values can also be obtained from subscriptions
     * to JSON topics.
     *
     * @param topicPath the topic path
     * @param message the JSON message to send
     * @param callback notifies message sent
     */
    public void send(String topicPath, JSON message, SendCallback callback) {
        messaging.send(topicPath, message, callback);
    }

    /**
     * Sends a JSON message to a specified topic path with context string.
     * <P>
     * A JSON value can be created from a string using:
     * {@code Diffusion.dataTypes().json().fromString(aString)}
     * <P>
     * JSON values can also be obtained from subscriptions
     * to JSON topics.
     * <P>
     * Callback will be directed to the contextual callback with the string
     * provided.
     *
     * @param topicPath the topic path
     * @param message the JSON message to send
     * @param context the context string to return with the callback
     * @param callback notifies message sent
     */
    public void send(
        String topicPath,
        JSON message,
        String context,
        SendContextCallback<String> callback) {

        messaging.send(topicPath, message, context, callback);
    }

    /**
     * Sends a JSON message to a specified topic path with headers.
     * <P>
     * A JSON value can be created from a string using:
     * {@code Diffusion.dataTypes().json().fromString(aString)}
     * <P>
     * JSON values can also be obtained from subscriptions
     * to JSON topics.
     * <P>
     * There will be no context with the message so callback will be directed to
     * the no context callback.
     *
     * @param topicPath the topic path
     * @param message the JSON message to send
     * @param headers the headers to send with the message
     * @param callback notifies message sent
     */
    public void sendWithHeaders(
        String topicPath,
        JSON message,
        List<String> headers,
        SendCallback callback) {

        messaging.send(
            topicPath,
            message,
            messaging.sendOptionsBuilder().headers(headers).build(),
            callback);
    }

    /**
     * Close the session.
     */
    public void close() {
        session.close();
    }

}
