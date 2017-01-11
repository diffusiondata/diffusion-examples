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

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pushtechnology.diffusion.client.Diffusion;
import com.pushtechnology.diffusion.client.content.Content;
import com.pushtechnology.diffusion.client.features.Messaging;
import com.pushtechnology.diffusion.client.features.control.topics.MessagingControl;
import com.pushtechnology.diffusion.client.features.control.topics.MessagingControl.MessageHandler;
import com.pushtechnology.diffusion.client.features.control.topics.MessagingControl.SendCallback;
import com.pushtechnology.diffusion.client.session.Session;
import com.pushtechnology.diffusion.client.session.SessionId;
import com.pushtechnology.diffusion.client.types.ReceiveContext;
import com.pushtechnology.diffusion.datatype.json.JSON;

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

    private final Session echoingSession;
    private final Session sendingSession;
    private final MessagingControl echoingSessionMessagingControl;
    private final MessagingControl sendingSessionMessagingControl;
    private final SendCallback sendCallback;

    private static final Logger LOG =
        LoggerFactory.getLogger(ControlClientReceivingMessages.class);

    /**
     * Constructor.
     *
     * @param callback for result of sends
     */
    public ControlClientReceivingMessages(SendCallback callback) {

        sendCallback = callback;

        echoingSession =
            Diffusion.sessions().principal("control").password("password")
                .open("ws://diffusion.example.com:80");

        sendingSession =
            Diffusion.sessions().principal("control").password("password")
                .open("ws://diffusion.example.com:80");

        echoingSessionMessagingControl = echoingSession.feature(MessagingControl.class);
        sendingSessionMessagingControl = sendingSession.feature(MessagingControl.class);

        // Register to receive all messages sent by clients on the "foo" branch
        // To do this, the client session must have the 'register_handler' permission.
        echoingSessionMessagingControl.addMessageHandler("foo", new EchoHandler());
    }

    /**
     * Close the session.
     */
    public void close() {
        echoingSession.close();
        sendingSession.close();
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

            try {
                final JSONObject jsonObject = new JSONObject(content.asString());
                final String value = (String) jsonObject.get("hello");
                LOG.info("JSON content with key: 'hello' and value: '{}'", value);
            }
            catch (JSONException e) {
                //Non-JSON message so just carry on and echo the message
            }

            // To send a message to a client, this client session must have
            // the 'view_session' and 'send_to_session' permissions.
            echoingSessionMessagingControl.send(
                sessionId,
                topicPath,
                content,
                echoingSessionMessagingControl.sendOptionsBuilder()
                    .headers(context.getHeaderList())
                    .build(),
                sendCallback);

        }
    }

    /**
     * Add a message stream to observe echoed messages.
     *
     * @param stream stream to be added
     */
    public void addSendingSessionMessageStream(Messaging.MessageStream stream) {
        sendingSession.feature(Messaging.class).addMessageStream("foo", stream);
    }

    /**
     * Sends messages "hello:world" and "{"hello":"world"}".
     */
    public void sendHelloWorld() {
        final Content helloWorldContent = Diffusion.content().newContent("hello:world");
        final JSON helloWorldJson = Diffusion.dataTypes().json().fromJsonString("{\"hello\":\"world\"}");

        //To do this, the client session must have the 'view_session' and 'send_to_session' permissions.
        sendingSessionMessagingControl.send(echoingSession.getSessionId(), "foo", helloWorldContent, sendCallback);
        sendingSessionMessagingControl.send(echoingSession.getSessionId(), "foo", helloWorldJson, sendCallback);
    }

}
