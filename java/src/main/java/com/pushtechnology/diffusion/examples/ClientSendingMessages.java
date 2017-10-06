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

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.pushtechnology.diffusion.client.Diffusion;
import com.pushtechnology.diffusion.client.features.Messaging;
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
     *
     * @param topicPath the topic path
     * @param message the message to send
     */
    public void send(String topicPath, String message)
        throws InterruptedException, ExecutionException, TimeoutException {
        messaging.send(topicPath, message).get(5, TimeUnit.SECONDS);
    }

    /**
     * Close the session.
     */
    public void close() {
        session.close();
    }

}
