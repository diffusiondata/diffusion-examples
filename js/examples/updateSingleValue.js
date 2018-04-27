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
    var TopicSpecification = diffusion.topics.TopicSpecification;
    var TopicType = diffusion.topics.TopicType;

    // A session may update any existing topic. Update values must be of the same type as the topic being updated.

    // Add a topic first with a string type
    session.topics.add('foo', new TopicSpecification(TopicType.STRING)).then(function() {
        // Update the topic
        return session.topics.updateValue('foo', 'hello', diffusion.datatypes.string());
    }).then(function() {
        // Update the topic again
        return session.topics.updateValue('foo', 'world', diffusion.datatypes.string());
    });

    // Add a topic with a double type
    session.topics.add('bar', new TopicSpecification(TopicType.DOUBLE)).then(function() {
        return session.topics.updateValue('bar', 123, diffusion.datatypes.double());
    }).then(function() {
        return session.topics.updateValue('bar', 456.789, diffusion.datatypes.double());
    });
});
