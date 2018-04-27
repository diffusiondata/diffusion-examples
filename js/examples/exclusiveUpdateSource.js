/*******************************************************************************
 * Copyright (C) 2018 Push Technology Ltd.
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
    // A session may establish an exclusive update source. Once active, only this session may update topics at or
    // under the registration branch.

    session.topics.registerUpdateSource('exclusive/topic', {
        onRegister : function(topic, deregister) {
            // The handler provides a deregistration function to remove this registration and allow other sessions to
            // update topics under the registered path.
        },
        onActive : function(topic, updater) {
            // Once active, a handler may use the provided updater to update any topics at or under the registered path
            updater.update('exclusive/topic/bar', 123).then(function() {
                // The update was successful.
            }, function(err) {
                // There was an error updating the topic
            });
        },
        onStandBy : function(topic) {
            // If there is another update source registered for the same topic path, any subsequent registrations will
            // be put into a standby state. The registration is still held by the server, and the 'onActive' function
            // will be called if the pre-existing registration is closed at a later point in time
        },
        onClose : function(topic, err) {
            // The 'onClose' function will be called once the registration is closed, either by the session being closed
            // or the 'deregister' function being called.
        }
    });
});
