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
    session.clients.registerSessionPropertiesListener(diffusion.clients.PropertyKeys.ALL_FIXED_PROPERTIES)
        .then(function() {
            console.log('Session properties listener request sent');

            var listener = session.clients.getSessionPropertiesListener();
            listener
                .on('onSessionOpen', function(event) {
                    console.log('onSessionOpen (' + event.sessionId + ')');
                    console.log('  properties=' + JSON.stringify(event.properties, null, 2));
                })
                .on('onSessionUpdate', function(event) {
                    console.log('onSessionUpdate (' + event.sessionId + ')');
                    console.log('  type=' + event.type);
                    console.log('  old properties=' + JSON.stringify(event.oldProperties, null, 2));
                    console.log('  new properties=' + JSON.stringify(event.newProperties, null, 2));
                })
                .on('onSessionClose', function(event) {
                    console.log('onSessionClose (' + event.sessionId + ')');
                    console.log('  reason=' + event.reason);
                    console.log('  properties=' + JSON.stringify(event.properties, null, 2));
                });
        }, function(err) {
            console.log('An error has occurred:', err);
        });

});
