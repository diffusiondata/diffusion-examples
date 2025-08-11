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
const { expectTopicCounts, PartiallyOrderedCheckpointTester } = require('../../../../../test/util');
/// end::log

export async function topicViewsApiList() {
    /// tag::log
    const check = new PartiallyOrderedCheckpointTester([
        [
            'Topic View topic_view_1: map my/topic/path to views/<path(0)> (ADMINISTRATOR)',
            'Topic View topic_view_2: map my/topic/path/array to views/<path(0)> (ADMINISTRATOR)'
        ]
    ]);
    /// end::log
    /// tag::topic_views_api_list[]
    const session = await diffusion.connect({
        host: 'localhost',
        port: 8080,
        principal: 'admin',
        credentials: 'password'
    });

    const specification = new diffusion.topics.TopicSpecification(diffusion.topics.TopicType.JSON);
    const jsonData = diffusion.datatypes.json().from({ diffusion: 'data' });
    await session.topicUpdate.set(
        'my/topic/path',
        diffusion.datatypes.json(),
        jsonData,
        { specification: specification }
    );

    const topicView1 = await session.topicViews.createTopicView(
        'topic_view_1',
        'map my/topic/path to views/<path(0)>'
    );
    console.log(`Topic View ${topicView1.name} has been created.`);

    await session.topicUpdate.set(
        'my/topic/path/array',
        diffusion.datatypes.json(),
        jsonData,
        { specification: specification }
    );

    const topicView2 = await session.topicViews.createTopicView(
        'topic_view_2',
        'map my/topic/path/array to views/<path(0)>'
    );
    console.log(`Topic View ${topicView2.name} has been created.`);

    const topicViews = await session.topicViews.listTopicViews();
    for (const topicView of topicViews) {
        console.log(`Topic View ${topicView.name}: ${topicView.specification} (${[...topicView.roles]})`);
        /// tag::log
        check.log(`Topic View ${topicView.name}: ${topicView.specification} (${[...topicView.roles]})`);
        /// end::log
    }

    await session.closeSession();
    /// end::topic_views_api_list[]
    /// tag::log
    await expectTopicCounts({
        'views/my/topic/path': 1,
        'views/my/topic/path/array': 1,

    });
    await check.done();
    /// end::log
}
