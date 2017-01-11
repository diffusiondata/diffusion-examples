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

var diffusion = require('diffusion');

// Connect to the server. Change these options to suit your own environment.
// Node.js does not accept self-signed certificates by default. If you have
// one of these, set the environment variable NODE_TLS_REJECT_UNAUTHORIZED=0
// before running this example.
diffusion.connect({
    host   : 'localhost',
    port   : 8080,
    secure : false,
    principal : 'control',
    credentials : 'password'
}).then(function(session) {

    // Get our own fixed properties
    session.clients.getSessionProperties(session.sessionID, diffusion.clients.PropertyKeys.ALL_FIXED_PROPERTIES)
        .then(function(props) {
            console.log('getSessionProperties returns properties:', props);
        }, function(err) {
            console.log('An error has occurred:', err);
        });

    // Register a listener for session properties
    session.clients.setSessionPropertiesListener(diffusion.clients.PropertyKeys.ALL_FIXED_PROPERTIES, {
        onActive : function(deregister) {
            console.log("Session properties listener opened");

            // Call deregister() to close this listener
        },
        onClose : function() {
            console.log("Session properties listener closed"); 
        },
        onSessionOpen : function(session, properties) {
            // Notification that a session has been opened
            console.log("Session opened: " + session, JSON.stringify(properties));
        },
        onSessionEvent : function(session, event, properties) {
            // Notification that a session's properties have changed
            switch (event) {
                case session.clients.SessionEventType.UPDATED :
                    console.log("Session updated: " + session, JSON.stringify(properties));
                    break;
                case session.clients.SessionEventType.DISCONNECTED :
                    console.log("Session disconnected: " + session, JSON.stringify(properties));
                    break;
                case session.clients.SessionEventType.RECONNECTED :
                    console.log("Session reconnected: " + session, JSON.stringify(properties));
                    break;
                case session.clients.SessionEventType.FAILED_OVER :
                    console.log("Session failed over: " + session, JSON.stringify(properties));
            }
        },
        onSessionClose : function(session, properties, reason) {
            console.log("Session closed: " + session + " reason: " + reason, JSON.stringify(properties));
        }
    }).then(function() {
        console.log("Session listener successfully registered");
    }, function(err) {
        console.log('An error occurred registering a session listener:', err);
    });
});
