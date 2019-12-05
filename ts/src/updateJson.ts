/*******************************************************************************
 * Copyright (C) 2019 Push Technology Ltd.
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

import { connect, datatypes, topics, Session } from 'diffusion';

// example showcasing how to update a JSON topic
export async function updateJsonExample() {

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

    const jsonDataType = datatypes.json();
    const TopicSpecification = topics.TopicSpecification;
    const TopicType = topics.TopicType;

    // A session may update any existing topic. Update values must be of the same type as the topic being updated.

    // Add a topic first with topic specification
    await session.topics.add('foo', new TopicSpecification(TopicType.JSON));

    // Update the topic with JSON content
    await session.topicUpdate.set('foo', jsonDataType, jsonDataType.from({ "hello": "bar", "foo": "world" }));

    // Update the topic again with JSON converted from a JSON string
    await session.topicUpdate.set('foo', jsonDataType, jsonDataType.fromJsonString("{ \"hello\": \"foo\", \"foo\": \"hello\" }"));

    // Update the topic again with a standard JavaScript JSON object
    await session.topicUpdate.set('foo', jsonDataType, { hello: "world", foo: "bar" });
}
