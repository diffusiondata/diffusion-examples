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
    // A session may update any existing topic. Update values must be of the same type as the topic being updated.

    // Add a topic first with a string type
    session.topics.add('foo', diffusion.topics.TopicType.STRING).then(function() {
        // Update the topic
        return session.topics.update('foo', 'hello');
    }).then(function() {
        // Update the topic again
        return session.topics.update('foo', 'world');
    });

    // Add a topic with a double type
    session.topics.add('bar', diffusion.topics.TopicType.DOUBLE).then(function() {
        return session.topics.update('bar', 123);
    }).then(function() {
        return session.topics.update('bar', 456.789);
    });
});
