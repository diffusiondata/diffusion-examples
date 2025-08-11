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
const { expectJsonTopicToHaveValue, PartiallyOrderedCheckpointTester } = require('../../../../../test/util');
/// end::log

export async function topicViewsDslPatchTransformationsCopy() {
    /// tag::log
    const check = new PartiallyOrderedCheckpointTester([[
        'Subscribed to views/my/topic/path'
    ]]);
    /// end::log
    /// tag::topic_views_dsl_patch_transformations_copy[]
    const session = await diffusion.connect({
        host: 'localhost',
        port: 8080,
        principal: 'admin',
        credentials: 'password'
    });

    const specification = new diffusion.topics.TopicSpecification(diffusion.topics.TopicType.JSON);
    const jsonData = diffusion.datatypes.json().from({
        'Meet the Flintstones': {
            Fred: 'Flintstone',
            Barne: 'Rubble'
        },
        'The Jetsons': {
            George: 'Jetson'
        }
    });
    await session.topicUpdate.set(
        'my/topic/path',
        diffusion.datatypes.json(),
        jsonData,
        { specification: specification }
    );

    const valueStream = session.addFallbackStream(diffusion.datatypes.json());
    valueStream.on({
        subscribe : (topic, specification) => {
            console.log(`Subscribed to ${topic}`);
            /// tag::log
            check.log(`Subscribed to ${topic}`);
            /// end::log
        },
        unsubscribe : (topic, specification, reason) => {},
        value : (topic, spec, newValue, oldValue) => {}
    });

    await session.select('?views//');

    const topicView = await session.topicViews.createTopicView(
        'topic_view_1',
        `map ?my/topic/path// to views/<path(0)> patch '[
            {
                "op": "copy",
                "from": "/Meet the Flintstones/Fred",
                "path": "/The Jetsons/Fred"
            }
        ]'`
    );
    console.log(`Topic View ${topicView.name} has been created.`);

    await session.closeSession();
    /// end::topic_views_dsl_patch_transformations_copy[]
    /// tag::log
    await expectJsonTopicToHaveValue('views/my/topic/path', {
        'Meet the Flintstones': {
            Fred: 'Flintstone',
            Barne: 'Rubble'
        },
        'The Jetsons': {
            Fred: 'Flintstone',
            George: 'Jetson'
        }
    });

    await check.done();
    /// end::log
}
