/*******************************************************************************
 * Copyright (C) 2024 Diffusion Data Ltd.
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

import { connect, datatypes, topics, topicUpdate } from 'diffusion';
/// tag::log
import { expectTopicCounts } from '../../../../test/util';
/// end::log

export async function pubSubRemoveMultipleTopicsViaSelector(): Promise<void> {
    /// tag::pub_sub_remove_multiple_topics_via_selector[]
    // Connect to the server.
    const session = await connect({
        host: 'localhost',
        port: 8080,
        principal: 'admin',
        credentials: 'password'
    });

    const specification = new topics.TopicSpecification(topics.TopicType.JSON);
    const jsonData1 = datatypes.json().from({ diffusion: [ 'data', 'more data' ] });
    const topicCreationResult1 = await session.topicUpdate.set(
        'my/topic/path/to/be/removed',
        datatypes.json(),
        jsonData1,
        { specification: specification }
    );
    if (topicCreationResult1 === topicUpdate.TopicCreationResult.CREATED) {
        console.log('Topic has been created.');
    } else {
        console.log('Topic already exists.');
    }
    /// tag::log
    expect(topicCreationResult1).toBe(topicUpdate.TopicCreationResult.CREATED);
    /// end::log

    const jsonData2 = datatypes.json().from({ diffusion: [ 'data', 'also more data' ] });
    const topicCreationResult2 = await session.topicUpdate.set(
        'my/topic/path/to/be/also/removed',
        datatypes.json(),
        jsonData2,
        { specification: specification }
    );
    if (topicCreationResult2 === topicUpdate.TopicCreationResult.CREATED) {
        console.log('Topic has been created.');
    } else {
        console.log('Topic already exists.');
    }
    /// tag::log
    expect(topicCreationResult2).toBe(topicUpdate.TopicCreationResult.CREATED);
    /// end::log

    const jsonData3 = datatypes.json().from({ diffusion: [ 'no data' ] });

    const topicCreationResult3 = await session.topicUpdate.set(
        'my/topic/path/will/not/be/removed',
        datatypes.json(),
        jsonData3,
        { specification: specification }
    );
    if (topicCreationResult3 === topicUpdate.TopicCreationResult.CREATED) {
        console.log('Topic has been created.');
    } else {
        console.log('Topic already exists.');
    }
    /// tag::log
    expect(topicCreationResult3).toBe(topicUpdate.TopicCreationResult.CREATED);
    /// end::log

    const jsonData4 = datatypes.json().from({ diffusion: [ 'no data either' ] });

    const topicCreationResult4 = await session.topicUpdate.set(
        'my/topic/path/will/not/be/removed/either',
        datatypes.json(),
        jsonData4,
        { specification: specification }
    );
    if (topicCreationResult4 === topicUpdate.TopicCreationResult.CREATED) {
        console.log('Topic has been created.');
    } else {
        console.log('Topic already exists.');
    }
    /// tag::log
    expect(topicCreationResult4).toBe(topicUpdate.TopicCreationResult.CREATED);
    /// end::log


    const removalResult = await session.topics.remove('?my/topic/path/to/be//');
    console.log(`${removalResult.removedCount} topics have been removed.`);

    await session.closeSession();
    /// end::pub_sub_remove_multiple_topics_via_selector[]
    /// tag::log
    await expectTopicCounts({
        'my/topic/path/to/be/removed': 0,
        'my/topic/path/to/be/also/removed': 0,
        'my/topic/path/will/not/be/removed': 1,
        'my/topic/path/will/not/be/removed/either': 1
    });
    /// end::log
}
