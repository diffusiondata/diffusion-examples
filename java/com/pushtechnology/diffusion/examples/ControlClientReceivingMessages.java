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

import com.pushtechnology.diffusion.client.Diffusion;
import com.pushtechnology.diffusion.client.content.Content;
import com.pushtechnology.diffusion.client.features.control.topics.MessagingControl;
import com.pushtechnology.diffusion.client.features.control.topics.MessagingControl.MessageHandler;
import com.pushtechnology.diffusion.client.features.control.topics.MessagingControl.SendCallback;
import com.pushtechnology.diffusion.client.session.Session;
import com.pushtechnology.diffusion.client.session.SessionId;
import com.pushtechnology.diffusion.client.types.ReceiveContext;

/**
 * This is an example of a control client using the 'MessagingControl' feature
 * to receive messages from clients and also send messages to clients.
 * <P>
 * It is a trivial example that simply responds to all messages on a particular
 * branch of the topic tree by echoing them back to the client exactly as they
 * are complete with headers.
 *
 * @author Push Technology Limited
 * @since 5.0
 */
public class ControlClientReceivingMessages {

    private final Session session;
    private final MessagingControl messagingControl;
    private final SendCallback sendCallback;

    /**
     * Constructor.
     *
     * @param callback for result of sends
     */
    public ControlClientReceivingMessages(SendCallback callback) {

        sendCallback = callback;

        session =
            Diffusion.sessions().principal("control").password("password")
                .open("ws://diffusion.example.com:80");
        messagingControl = session.feature(MessagingControl.class);

        // Register to receive all messages sent by clients on the "foo" branch
        // To do this, the client session must have the 'register_handler' permission.
        messagingControl.addMessageHandler("foo", new EchoHandler());
    }

    /**
     * Close the session.
     */
    public void close() {
        session.close();
    }

    /**
     * Handler that echoes messages back to the originating client complete with
     * original headers.
     */
    private class EchoHandler extends MessageHandler.Default {
        @Override
        public void onMessage(
            SessionId sessionId,
            String topicPath,
            Content content,
            ReceiveContext context) {

            // To send a message to a client, this client session must have
            // the 'view_session' and 'send_to_session' permissions.
            messagingControl.send(
                sessionId,
                topicPath,
                content,
                messagingControl.sendOptionsBuilder()
                    .headers(context.getHeaderList())
                    .build(),
                sendCallback);

        }
    }

}
