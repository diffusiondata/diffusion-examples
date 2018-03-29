/*******************************************************************************
 * Copyright (C) 2014, 2018 Push Technology Ltd.
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

import static com.pushtechnology.diffusion.client.features.control.clients.MessageQueuePolicy.ThrottlerType.MESSAGE_INTERVAL;

import com.pushtechnology.diffusion.client.Diffusion;
import com.pushtechnology.diffusion.client.features.control.clients.ClientControl;
import com.pushtechnology.diffusion.client.features.control.clients.ClientControl.ClientCallback;
import com.pushtechnology.diffusion.client.features.control.clients.ClientControl.QueueEventHandler;
import com.pushtechnology.diffusion.client.features.control.clients.MessageQueuePolicy;
import com.pushtechnology.diffusion.client.session.Session;
import com.pushtechnology.diffusion.client.session.SessionId;

/**
 * This demonstrates the use of a control client to apply both throttling and
 * conflation to clients. It throttles and conflates all clients that reach
 * their queue thresholds.
 * <P>
 * This uses the 'ClientControl' feature.
 *
 * @author Push Technology Limited
 * @since 5.0
 */
public class ControlClientConflateAndThrottle {

    private final Session session;
    private final ClientControl clientControl;
    private final ClientCallback clientCallback;

    /**
     * Constructor.
     *
     * @param callback notifies callback from throttle requests
     */
    public ControlClientConflateAndThrottle(ClientCallback callback) {

        session =
            Diffusion.sessions().principal("control").password("password")
                .open("ws://diffusion.example.com:80");

        // Create the ClientControl feature with a handler that sets queue
        // thresholds on new connecting clients and sets a listener for queue
        // events.
        clientControl = session.feature(ClientControl.class);
        clientCallback = callback;

        // To register a queue event handler, the client session must have
        // the 'register handler' and 'view_session' permissions.
        clientControl.setQueueEventHandler(new MyThresholdHandler());
    }

    /**
     * Close the session.
     */
    public void close() {
        session.close();
    }

    private class MyThresholdHandler extends QueueEventHandler.Default {

        @Override
        public void onLowerThresholdCrossed(
            final SessionId client,
            final MessageQueuePolicy policy) {

            // The setConflated method enables conflation.
            // The default configuration enables conflation for sessions.
            clientControl
                .setConflated(session.getSessionId(), true, clientCallback);

            // The setThrottled method enables throttling.
            // This method requires the client session to have the
            // 'modify_session' permission.
            clientControl
                .setThrottled(client, MESSAGE_INTERVAL, 1000, clientCallback);
        }

        @Override
        public void onUpperThresholdCrossed(
            final SessionId client,
            final MessageQueuePolicy policy) {

            // Conflation remains enabled from when the lower threshold was
            // crossed.

            // The setThrottled method replacing the current throttler with a
            // more aggressive limit.
            // This method requires the client session to have the
            // 'modify_session' permission.
            clientControl
                .setThrottled(client, MESSAGE_INTERVAL, 10, clientCallback);
        }
    }
}
