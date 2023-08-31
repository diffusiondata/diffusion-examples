/*******************************************************************************
 * Copyright (C) 2019 - 2023 DiffusionData Ltd.
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

import { connect, clients, Session, SessionProperties, SessionId, SessionEventType } from 'diffusion';

// example showcasing how a control session can receive updates on another session's properties
export async function clientControlExample(): Promise<void> {

    // Connect to the server. Change these options to suit your own environment.
    // Node.js does not accept self-signed certificates by default. If you have
    // one of these, set the environment variable NODE_TLS_REJECT_UNAUTHORIZED=0
    // before running this example.
    const session: Session = await connect({
        host: 'localhost',
        port: 8080,
        secure: false,
        principal: 'control',
        credentials: 'password'
    });

    try {
        // Get our own fixed properties
        const props: SessionProperties = await session.clients.getSessionProperties(session.sessionId, clients.PropertyKeys.ALL_FIXED_PROPERTIES);
        console.log('getSessionProperties returns properties:', props);
    } catch (error) {
        console.log('An error has occurred:', error);
    }

    try {
        // Register a listener for session properties
        await session.clients.setSessionPropertiesListener(clients.PropertyKeys.ALL_FIXED_PROPERTIES, {
            onActive: (deregister) => {
                console.log("Session properties listener opened");

                // `deregister` is a callback passed to the listener when it becomes active.
                // A call to deregister() will close this listener.
            },
            onClose: () => {
                console.log("Session properties listener closed");
            },
            onSessionOpen: (sessionId: SessionId, properties: SessionProperties) =>{
                // Notification that a session has been opened
                console.log("Session opened: " + session, JSON.stringify(properties));
            },
            onSessionEvent: (sessionId: SessionId, event: SessionEventType, properties: SessionProperties) => {
                // Notification that a session's properties have changed
                switch (event) {
                case session.clients.SessionEventType.UPDATED:
                    console.log("Session updated: " + sessionId, JSON.stringify(properties));
                    break;
                case session.clients.SessionEventType.DISCONNECTED:
                    console.log("Session disconnected: " + sessionId, JSON.stringify(properties));
                    break;
                case session.clients.SessionEventType.RECONNECTED:
                    console.log("Session reconnected: " + sessionId, JSON.stringify(properties));
                    break;
                case session.clients.SessionEventType.FAILED_OVER:
                    console.log("Session failed over: " + sessionId, JSON.stringify(properties));
                }
            },
            onSessionClose: (sessionId: SessionId, properties: SessionProperties, reason) => {
                console.log("Session closed: " + session + " reason: " + reason, JSON.stringify(properties));
            },
            onError: (error) => {
                console.log('An error has occurred:', error);
            }
        });

        console.log("Session listener successfully registered");
    } catch (error) {
        console.log('An error occurred registering a session listener:', error);
    }
}
