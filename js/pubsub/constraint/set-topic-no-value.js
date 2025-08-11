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

const diffusion = require('diffusion');
/// tag::log
const { expectJsonTopicToHaveValue, expectTopicExists } = require('../../../../test/util');
/// end::log

export async function setTopicNoValueConstraintExample() {
    /// tag::pub_sub_publish_with_constraint_set_topic_no_value[]
    // Connect to the server.
    const session = await diffusion.connect({
        host: 'localhost',
        port: 8080,
        principal: 'admin',
        credentials: 'password'
    });

    try {
        await session.topics.add('my/topic/path', diffusion.topics.TopicType.JSON);

        const jsonData = diffusion.datatypes.json().from({
            diffusion: ['data', 'more data']
        });

        const updateConstraint = diffusion.updateConstraints().noValue();

        await session.topicUpdate.set(
            'my/topic/path',
            diffusion.datatypes.json(),
            jsonData,
            { constraint: updateConstraint }
        );

        console.log('Topic value has been set.');
    } catch (err) {
        console.error('Topic value could not be set.', err);
        throw err;
    }

    await session.closeSession();
    /// end::pub_sub_publish_with_constraint_set_topic_no_value[]
    /// tag::log
    await expectJsonTopicToHaveValue('my/topic/path', { diffusion: ['data', 'more data'] });
    /// end::log
}
