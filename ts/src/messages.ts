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

import { connect, datatypes, topics, Session, RequestHandler, Responder, RequestContext } from 'diffusion';

// example showcasing how to send messages to a single session
export async function messagesExample(): Promise<void> {

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

    // 1. Messages can be sent & received between sessions.
    // Create a request handler that handles strings
    const handler: RequestHandler = {
        onRequest: function(request, context: RequestContext, responder: Responder) {
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

    try {

        // Register the handler
        await session.messages.addRequestHandler('foo/bar', handler);
        console.log("Request handler has been added");
    } catch (error) {
        console.log('Failed to register request handler: ', error);
    }

    // 2. Messages can be sent & received between sessions.

    // Send a message to another session. It is the application's responsibility
    // to find the SessionID of the intended recipient.
    const responseFoo = await session.messages.sendRequest('foo/bar', 'Hello World', 'another-session-id', datatypes.string(), datatypes.string());
    console.log("Received response " + responseFoo);

    // 3. Messages can also be sent without a recipient, in which case they will be dispatched to any Message Handlers
    // that have been registered for the same path. If multiple handlers are registered to the same path, any given
    // message will only be dispatched to one handler.

    // Send a message at a lower path, without an explicit recipient - this will be received by the Handler.
    const responseBar = await session.messages.sendRequest('foo/bar', 'Hello World', datatypes.string(), datatypes.string());
    console.log("Received response " + responseBar);

    // 4. The datatype of the message and the response can be specified using topic types or omitted altogether.
    // In the latter case, the datatype is inferred from the value passed to sendRequest()

    // Send a message using topic types to specify the datatype
    const responseBaz = await session.messages.sendRequest('foo/bar', 'Hello World', topics.TopicType.STRING, topics.TopicType.STRING);
    console.log("Received response " + responseBaz);

    // Send a message leaving out the datatype
    const responseQux = await session.messages.sendRequest('foo/bar', 'Hello World')
    console.log("Received response " + responseQux);
}
