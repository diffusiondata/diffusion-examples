/*******************************************************************************
 * Copyright (C) 2014, 2015 Push Technology Ltd.
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

/**
 * This is a simple example of a client that uses the 'Messaging' feature to
 * send messages to a topic path.
 * <P>
 * To send a message on a topic path, the client session requires the
 * 'send_to_message_handler' permission.
 *
 * @author Push Technology Limited
 * @since 5.0
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
     * Sends a simple string message to a specified topic path.
     * <P>
     * There will be no context with the message so callback will be directed to
     * the no context callback.
     *
     * @param topicPath the topic path
     * @param message the message to send
     * @param callback notifies message sent
     */
    public void send(String topicPath, String message, SendCallback callback) {
        messaging.send(
            topicPath,
            Diffusion.content().newContent(message),
            callback);
    }

    /**
     * Sends a simple string message to a specified topic path with context string.
     * <P>
     * Callback will be directed to the contextual callback with the string
     * provided.
     *
     * @param topicPath the topic path
     * @param message the message to send
     * @param context the context string to return with the callback
     * @param callback notifies message sent
     */
    public void send(
        String topicPath,
        String message,
        String context,
        SendContextCallback<String> callback) {

        messaging.send(
            topicPath,
            Diffusion.content().newContent(message),
            context,
            callback);
    }

    /**
     * Sends a string message to a specified topic path with headers.
     * <P>
     * There will be no context with the message so callback will be directed to
     * the no context callback.
     *
     * @param topicPath the topic path
     * @param message the message to send
     * @param headers the headers to send with the message
     * @param callback notifies message sent
     */
    public void sendWithHeaders(
        String topicPath,
        String message,
        List<String> headers,
        SendCallback callback) {

        messaging.send(
            topicPath,
            Diffusion.content().newContent(message),
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
