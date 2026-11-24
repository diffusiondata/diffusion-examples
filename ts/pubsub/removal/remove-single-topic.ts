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

export async function pubSubRemoveSingleTopicViaPath(): Promise<void> {
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

    const jsonData2 = datatypes.json().from({ diffusion: [ 'no data' ] });
    const topicCreationResult2 = await session.topicUpdate.set(
        'my/topic/path/will/not/be/removed',
        datatypes.json(),
        jsonData2,
        { specification: specification }
    );
    if (topicCreationResult2 === topicUpdate.TopicCreationResult.CREATED) {
        console.log('Topic has been created.');
    } else {
        console.log('Topic already exists.');
    }

    const jsonData3 = datatypes.json().from({ diffusion: [ 'no data either' ] });

    const topicCreationResult3 = await session.topicUpdate.set(
        'my/topic/path/will/not/be/removed/either',
        datatypes.json(),
        jsonData3,
        { specification: specification }
    );
    if (topicCreationResult3 === topicUpdate.TopicCreationResult.CREATED) {
        console.log('Topic has been created.');
    } else {
        console.log('Topic already exists.');
    }

    await session.topics.remove('my/topic/path/to/be/removed');
    console.log('Topic has been removed.');

    await session.closeSession();
}
