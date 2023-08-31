/*******************************************************************************
 * Copyright (C) 2018 - 2023 DiffusionData Ltd.
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

var diffusion = require('diffusion');

// Connect to the server. Change these options to suit your own environment.
// Node.js will not accept self-signed certificates by default. If you have
// one of these, set the environment variable NODE_TLS_REJECT_UNAUTHORIZED=0
// before running this example.
diffusion.connect({
    host   : 'diffusion.example.com',
    port   : 443,
    secure : true,
    principal : 'control',
    credentials : 'password'
}).then(function(session) {

    // 1. Messages can be sent & received between sessions.
    // Create a request handler that handles strings
    var handler = {
        onRequest: function(request, context, responder) {
            console.log('Received request: ' + request); // Log the request
            responder.respond('something');
        },
        onError: function() {
            console.log('An error occurred');
        },
        onClose: function() {
            console.log('Handler closed');
        }
    };

    // Register the handler
    session.messages.addRequestHandler('foo/bar', handler).then(function() {
        console.log("Request handler has been added")
    }, function(error) {
        console.log('Failed to register request handler: ', e);
    });

    // 2. Messages can be sent & received between sessions.

    // Send a message to another session. It is the application's responsibility
    // to find the SessionID of the intended recipient.
    session.messages.sendRequest('foo/bar', 'Hello World', 'another-session-id', diffusion.datatypes.string(), diffusion.datatypes.string())
    .then(function(response) {
        console.log("Received response " + response);
    });

    // 3. Messages can also be sent without a recipient, in which case they will be dispatched to any Message Handlers
    // that have been registered for the same path. If multiple handlers are registered to the same path, any given
    // message will only be dispatched to one handler.

    // Send a message at a lower path, without an explicit recipient - this will be received by the Handler.
    session.messages.sendRequest('foo/bar', 'Hello World', diffusion.datatypes.string(), diffusion.datatypes.string())
    .then(function(response) {
        console.log("Received response " + response);
    });

    // 4. The datatype of the message and the response can be specified using topic types or omitted altogether.
    // In the latter case, the datatype is inferred from the value passed to sendRequest()

    // Send a message using topic types to specify the datatype
    session.messages.sendRequest('foo/bar', 'Hello World', diffusion.topics.TopicType.STRING, diffusion.topics.TopicType.STRING)
    .then(function(response) {
        console.log("Received response " + response);
    });

    // Send a message leaving out the datatype
    session.messages.sendRequest('foo/bar', 'Hello World')
    .then(function(response) {
        console.log("Received response " + response);
    });
});
