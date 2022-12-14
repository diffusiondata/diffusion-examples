/*******************************************************************************
 * Copyright (C) 2019 - 2022 Push Technology Ltd.
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

import { connect, datatypes, topics, Session, SessionId, RequestStream, Responder, FilteredResponseHandler } from 'diffusion';

// example showcasing how to send messages to multiple sessions using a filter
export async function messagesFilterExample(): Promise<void> {

    // Connect to the server. Change these options to suit your own environment.
    // Node.js will not accept self-signed certificates by default. If you have
    // one of these, set the environment variable NODE_TLS_REJECT_UNAUTHORIZED=0
    // before running this example.
    const session: Session = await connect({
        host: 'diffusion.example.com',
        port: 443,
        secure: true,
        principal: 'control',
        credentials: 'password'
    });

    // Create a request handler that handles strings
    const requestSream: RequestStream = {
        onRequest: function(path: string, request, responder: Responder) {
            console.log('Received request: ' + request); // Log the request
            responder.respond('confirmation of request ' + request);
        },
        onError: function() {
            console.log('An error occurred');
        },
        onClose: function() {
            console.log('Handler closed');
        }
    };

    // Register the stream
    session.messages.setRequestStream('foo/bar', requestSream);

    // Send a message to another session listening on 'foo' by way of
    // session properties.
    const responseHandler: FilteredResponseHandler = {
        onResponse: function(sessionID: SessionId, response) {
            console.log("Received response " + response);
        },
        onResponseError: function() {
            console.log("There was an error when receiving the response");
        },
    };

    session.messages.sendRequestToFilter('$Principal is "control"', 'foo/bar', 'Hello world', responseHandler);
}
