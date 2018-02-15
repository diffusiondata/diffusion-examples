/*******************************************************************************
 * Copyright (C) 2015 Push Technology Ltd.
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
import com.pushtechnology.diffusion.client.features.control.topics.MessagingControl.SendToFilterCallback;
import com.pushtechnology.diffusion.client.session.Session;
import com.pushtechnology.diffusion.client.session.SessionId;
import com.pushtechnology.diffusion.client.types.ReceiveContext;

/**
 * This is an example of a control client using the 'MessagingControl' feature
 * to send messages to clients using message filters. It also demonstrates the
 * ability to register a message handler with an interest in session property
 * values.
 *
 * @author Push Technology Limited
 * @since 5.5
 */
public final class ControlClientUsingFiltersAndProperties {

    private final Session session;
    private final MessagingControl messagingControl;
    private final SendToFilterCallback sendToFilterCallback;

    /**
     * Constructor.
     *
     * @param callback for result of sends
     */
    public ControlClientUsingFiltersAndProperties(SendToFilterCallback callback) {

        sendToFilterCallback = callback;

        session =
            Diffusion.sessions().principal("control").password("password")
                .open("ws://diffusion.example.com:80");
        messagingControl = session.feature(MessagingControl.class);

        // Register to receive all messages sent by clients on the "foo" branch
        // and include the "JobTitle" session property value with each message.
        // To do this, the client session must have the 'register_handler'
        // permission.
        messagingControl.addMessageHandler(
            "foo", new BroadcastHandler(), "JobTitle");
    }

    /**
     * Close the session.
     */
    public void close() {
        session.close();
    }

    /**
     * Handler that will pass any message to all sessions that have a "JobTitle"
     * property set to "Staff" if, and only if it comes from a session that has
     * a "JobTitle" set to "Manager".
     */
    private class BroadcastHandler extends MessageHandler.Default {
        @Override
        public void onMessage(
            SessionId sessionId,
            String topicPath,
            Content content,
            ReceiveContext context) {

            if ("Manager".equals(context.getSessionProperties().get("JobTitle"))) {
                messagingControl.sendToFilter(
                    "JobTitle is 'Staff'",
                    topicPath,
                    content,
                    sendToFilterCallback);
            }

        }
    }

}
