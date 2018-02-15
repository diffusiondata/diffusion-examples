/*******************************************************************************
 * Copyright (C) 2017 Push Technology Ltd.
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
    
    // Create a stream of received messages for a specific path
    session.messages.listen('foo').on('message', function(msg) {
        console.log('Received message: ' + msg.content);
    });

    // Send a message to another session. It is the application's responsibility to find the SessionID of the intended
    // recipient.
    session.messages.send('foo', 'Hello world', 'another-session-id');

    // 2. Messages can also be sent without a recipient, in which case they will be dispatched to any Message Handlers
    // that have been registered for the same path. If multiple handlers are registered to the same path, any given
    // message will only be dispatched to one handler.
    
    // Register the handler to receive messages at or below the given path. 
    session.messages.addHandler('foo', {
        onActive : function() {
            console.log('Handler registered');
        },
        onClose : function() {
            console.log('Handler closed');
        },
        onMessage : function(msg) {
            console.log('Received message:' + msg.content + ' from Session: ' + msg.session);
            if (msg.properties) {
                console.log('with properties:', msg.properties);
            }
        }
    }).then(function() {
        console.log('Registered handler');
    }, function(e) {
        console.log('Failed to register handler: ', e);
    });

    // Send a message at a lower path, without an explicit recipient - this will be received by the Handler.
    session.messages.send('foo/bar', 'Another message');
});
