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

import { connect, datatypes, topics, topicUpdate, updateConstraints } from 'diffusion';

export async function addTopicAndSetAndConstraintExample(): Promise<void> {
    // Connect to the server.
    const session = await connect({
        host: 'localhost',
        port: 8080,
        principal: 'admin',
        credentials: 'password'
    });

    try {
        const specification = new topics.TopicSpecification(topics.TopicType.JSON);
        const jsonData = datatypes.json().from({ diffusion: 'data' });

        const topicCreationResult = await session.topicUpdate.set(
            'my/topic/path',
            datatypes.json(),
            jsonData,
            { specification: specification }
        );

        if (topicCreationResult === topicUpdate.TopicCreationResult.CREATED) {
            console.log('Topic has been created.');
        } else {
            console.log('Topic already exists.');
        }

        const updateConstraintA = updateConstraints()
            .jsonValue()
            .without('/bar');
        const updateConstraintB = updateConstraints()
            .jsonValue()
            .with('/diffusion', 'data', topicUpdate.UpdateConstraintOperator.IS);

        const updateConstraint = updateConstraintA.and(updateConstraintB);
        const newJsonData = datatypes.json().from({ diffusion: 'baz' });

        await session.topicUpdate.set(
            'my/topic/path',
            datatypes.json(),
            newJsonData,
            { constraint: updateConstraint }
        );
    } catch (err) {
        console.error('Topic value could not be set.', err);
        throw err;
    }

    await session.closeSession();
}
