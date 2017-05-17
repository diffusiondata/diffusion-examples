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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pushtechnology.diffusion.client.Diffusion;
import com.pushtechnology.diffusion.client.content.Content;
import com.pushtechnology.diffusion.client.features.control.topics.MessagingControl;
import com.pushtechnology.diffusion.client.features.control.topics.MessagingControl.MessageHandler;
import com.pushtechnology.diffusion.client.session.Session;
import com.pushtechnology.diffusion.client.session.SessionId;
import com.pushtechnology.diffusion.client.types.ReceiveContext;
import com.pushtechnology.diffusion.datatype.json.JSON;

/**
 * This is an example of a control client using the 'MessagingControl' feature
 * to receive JSON messages from clients.
 *
 * @author Push Technology Limited
 * @since 5.0
 * @see ClientSendingMessages
 */
public class ControlClientReceivingMessages {

    private final Session session;

    private static final Logger LOG = LoggerFactory.getLogger(ControlClientReceivingMessages.class);

    /**
     * Constructor.
     */
    public ControlClientReceivingMessages() {

        session =
            Diffusion.sessions().principal("control").password("password")
                .open("ws://diffusion.example.com:80");

        final MessagingControl messagingControl = session.feature(MessagingControl.class);

        // Register to receive all messages sent by clients on the "foo" branch
        // To do this, the client session must have the 'register_handler' permission.
        messagingControl.addMessageHandler("foo", new ExampleMessageHandler());
    }

    /**
     * Close the session.
     */
    public void close() {
        session.close();
    }

    /**
     * Handler that logs received JSON messages complete with original headers.
     */
    private class ExampleMessageHandler extends MessageHandler.Default {
        @Override
        public void onMessage(
            SessionId sessionId,
            String topicPath,
            Content content,
            ReceiveContext context) {

            final JSON json = Diffusion.dataTypes().json().readValue(content);
            LOG.info("Received JSON message: {}", json.toJsonString());
        }
    }
}
