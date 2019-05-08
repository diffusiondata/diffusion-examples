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
}).then(async function(session) {

    var TopicSpecification = diffusion.topics.TopicSpecification;
    var TopicType = diffusion.topics.TopicType;

    // 1. Like session.topics.add(), remove returns a promise, so we can chain together calls.
    await session.topics.add('foo', new TopicSpecification(TopicType.STRING));
    try {
        await session.topics.remove('foo');
        console.log('Removed topic foo');
    } catch (reason) {
        console.log('Failed to remove topic foo: ', reason);
    }

    // 2. Removing a topic will not remove any topics underneath it.

    // Add a hierarchy of topics.
    await Promise.all([
        session.topics.add('a', new TopicSpecification(TopicType.STRING),
        session.topics.add('a/b', new TopicSpecification(TopicType.STRING)),
        session.topics.add('a/b/c', new TopicSpecification(TopicType.STRING)),
        session.topics.add('a/b/c/d', new TopicSpecification(TopicType.STRING))
    ]);

    // Wait until we've removed the root topics
    await session.topics.remove('a');

    // Child topic still exists
    await session.topicUpdate.set('a/b', datatypes.string(), 'hello');

    // Removing all topics using a topic selector expression
    await session.topics.remove('?a//');

    console.log('Removed all topics including & under "a"');
});
