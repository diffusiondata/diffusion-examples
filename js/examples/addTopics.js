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
// Node.js does not accept self-signed certificates by default. If you have
// one of these, set the environment variable NODE_TLS_REJECT_UNAUTHORIZED=0
// before running this example.
diffusion.connect({
    host   : 'diffusion.example.com',
    port   : 443,
    secure : true,
    principal : 'control',
    credentials : 'password'
}).then(function(session) {
    // 1. Topics can be created with a topic path and a TopicType or TopicSpecification. The '/' delimiter allows topics
    // to be created in a hierarchy.

    var TopicSpecification = diffusion.topics.TopicSpecification;
    var TopicType = diffusion.topics.TopicType;

    // Create a topic from a topic type
    session.topics.add('topic/string', new TopicSpecification(TopicType.STRING));

    // Create a topic from a topic specification, with optional properties
    session.topics.add('topic/integer', new TopicSpecification(TopicType.INT64, {
        VALIDATES_VALUES : "true"
    }));

    // 2. Adding a topic returns a result, which allows us to handle when the operation has either completed
    // successfully or encountered an error.
    session.topics.add('topic/result', new TopicSpecification(TopicType.JSON)).then(function(result) {
        console.log('Added topic: ' + result.topic);
    }, function(reason) {
        console.log('Failed to add topic: ', reason);
    });

    // Adding a topic that already exists will succeed, so long as it has the same value type
    session.topics.add('topic/result', new TopicSpecification(TopicType.JSON)).then(function(result) {
        // result.added will be false, as the topic already existed
        console.log('Added topic: ' + result.topic, result.added);
    });

    // 3. Because the result returned from adding a topic is a promise, we can easily chain multiple topic adds together
    session.topics.add('chain/foo', new TopicSpecification(TopicType.STRING))
        .then(session.topics.add('chain/bar', new TopicSpecification(TopicType.STRING)))
        .then(session.topics.add('chain/baz', new TopicSpecification(TopicType.STRING)))
        .then(session.topics.add('chain/bob', new TopicSpecification(TopicType.STRING)))
        .then(function() {
            console.log('Added all topics');
        }, function(reason) {
            console.log('Failed to add topic', reason);
        });
});
