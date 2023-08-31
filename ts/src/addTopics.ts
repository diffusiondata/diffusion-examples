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

import { connect, topics, Session, TopicAddResult, TopicSpecification } from 'diffusion';

// example showcasing how to add a topic using session.topics.add
export async function addTopicExample(): Promise<void> {
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

    // 1. Topics can be created with a topic path and a TopicType or TopicSpecification. The '/' delimiter allows topics
    // to be created in a hierarchy.

    const TopicType = topics.TopicType;

    // Create a topic from a topic type
    session.topics.add('topic/string', new TopicSpecification(TopicType.STRING));

    // Create a topic from a topic specification, with optional properties
    session.topics.add('topic/integer', new TopicSpecification(TopicType.INT64, {
        VALIDATES_VALUES: "true"
    }));

    try {
        // 2. Adding a topic returns a promise, which allows us to handle when the operation has either completed
        // successfully or encountered an error.
        const result: TopicAddResult = await session.topics.add('topic/result', new TopicSpecification(TopicType.JSON));

        // result.added will be true
        console.log('Added topic: ' + result.topic, result.added);
    } catch (error) {
        console.log('Failed to add topic: ', error);
    }

    try {
        // Adding a topic that already exists will succeed, so long as it has the same value type
        const result: TopicAddResult = await session.topics.add('topic/result', new TopicSpecification(TopicType.JSON));

        // result.added will be false, as the topic already existed
        console.log('Added topic: ' + result.topic, result.added);
    } catch (error) {
        console.log('Failed to add topic: ', error);
    }

    try {
        // 3. Because the result returned from adding a topic is a promise, we can run multiple operations and wait for all
        // of them to finish
        const promise1: Promise<TopicAddResult> =  session.topics.add('chain/foo', new TopicSpecification(TopicType.STRING));
        const promise2: Promise<TopicAddResult> =  session.topics.add('chain/bar', new TopicSpecification(TopicType.STRING));
        const promise3: Promise<TopicAddResult> =  session.topics.add('chain/baz', new TopicSpecification(TopicType.STRING));
        const promise4: Promise<TopicAddResult> =  session.topics.add('chain/qux', new TopicSpecification(TopicType.STRING));

        await Promise.all([promise1, promise2, promise3, promise4]);

        console.log('Added all topics');
    } catch (error) {
        console.log('Failed to add topic: ', error);
    }
}
