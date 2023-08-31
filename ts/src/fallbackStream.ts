/*******************************************************************************
 * Copyright (C) 2019 - 2023 DiffusionData Ltd.
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

// example showcasing how to add a fallback stream that receives updates for topics that
// don't have a value stream
export async function fallbackStreamExample(): Promise<void> {

    // Data Types are exposed from the top level Diffusion namespace. It is often easier
    // to assign these directly to a local variable.
    const stringDataType = datatypes.string();
    const TopicSpecification = topics.TopicSpecification;
    const TopicType = topics.TopicType;

    // Connect to the server. Change these options to suit your own environment.
    // Node.js does not accept self-signed certificates by default. If you have
    // one of these, set the environment variable NODE_TLS_REJECT_UNAUTHORIZED=0
    // before running this example.
    const session: Session = await connect({
        host: 'diffusion.example.com',
        port: 443,
        secure: true,
        principal: 'control',
        credentials: 'password'
    });

    try {
        // 1. Create a String topic type
        await session.topics.add('topic/foo', new TopicSpecification(TopicType.STRING));
        // Once the topic is added successfully then update it with a new value
        await session.topicUpdate.set('topic/foo', stringDataType, "foo-string");
    } catch (error) {
        console.log("Fail to update topic 'foo': ", error);
    }

    try {
        // Create another String topic type
        await session.topics.add('topic/bar', new TopicSpecification(TopicType.STRING));
        await session.topicUpdate.set('topic/bar', stringDataType, 'bar-string');
    } catch (error) {
        console.log("Fail to update topic 'bar': ", error);
    }

    try {
        // And another String topic type
        await session.topics.add('topic/baz', new TopicSpecification(TopicType.STRING));
        await session.topicUpdate.set('topic/baz', stringDataType, "baz-string");
    } catch (error) {
        console.log("Fail to update topic 'baz': ", error);
    }

    // 2. Register a value stream for receiving String value update
    session.addStream('topic/foo', stringDataType)
        .on('value', function(topic, specification, newValue, oldValue) {
            console.log("Received update ", newValue);
        });


    // 3. Register a fallback value stream to receive update for any topics that don't have a stream register
    // In this example, the falback stream will receive updates from 'topic/bar' and 'topic/baz'
    session.addFallbackStream(stringDataType)
        .on('value', function(topic, specification, newValue, oldValue) {
            console.log("Received update ", newValue);
        });

    // 4. Subscribe
    session.select('?topic//');
}
