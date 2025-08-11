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

import { connect, datatypes, topics } from 'diffusion';
/// tag::log
import { expectJsonTopicToHaveValue, PartiallyOrderedCheckpointTester } from '../../../../../test/util';
/// end::log

export async function topicViewsApiRemove(): Promise<void> {
    /// tag::log
    const check = new PartiallyOrderedCheckpointTester([
        [
            'Topic View topic_view_1: map my/topic/path to views/<path(0)> (ADMINISTRATOR)',
            'Topic View topic_view_2: map my/topic/path/array to views/<path(0)> (ADMINISTRATOR)'
        ],
        [
            'Topic View topic_view_2: map my/topic/path/array to views/<path(0)> (ADMINISTRATOR)'
        ]
    ]);
    /// end::log
    /// tag::topic_views_api_remove[]
    // Connect to the server.
    const session = await connect({
        host: 'localhost',
        port: 8080,
        principal: 'admin',
        credentials: 'password'
    });

    const specification = new topics.TopicSpecification(topics.TopicType.JSON);
    const jsonData = datatypes.json().from({ diffusion: 'data' });

    await session.topicUpdate.set(
        'my/topic/path',
        datatypes.json(),
        jsonData,
        { specification: specification }
    );

    const topicView1 = await session.topicViews.createTopicView(
        'topic_view_1',
        'map my/topic/path to views/<path(0)>'
    );
    console.log(`Topic View ${topicView1.name} has been created.`);
    /// tag::log
    await expectJsonTopicToHaveValue('views/my/topic/path', { diffusion: 'data' });
    /// end::log

    await session.topicUpdate.set(
        'my/topic/path/array',
        datatypes.json(),
        jsonData,
        { specification: specification }
    );

    const topicView2 = await session.topicViews.createTopicView(
        'topic_view_2',
        'map my/topic/path/array to views/<path(0)>'
    );
    console.log(`Topic View ${topicView2.name} has been created.`);
    /// tag::log
    await expectJsonTopicToHaveValue('views/my/topic/path/array', { diffusion: 'data' });
    /// end::log

    const topicViews1 = await session.topicViews.listTopicViews();
    for (const topicView of topicViews1) {
        console.log(`Topic View ${topicView.name}: ${topicView.specification} (${[...topicView.roles]})`);
        /// tag::log
        check.log(`Topic View ${topicView.name}: ${topicView.specification} (${[...topicView.roles]})`);
        /// end::log
    }

    await session.topicViews.removeTopicView('topic_view_1');

    const topicViews2 = await session.topicViews.listTopicViews();
    for (const topicView of topicViews2) {
        console.log(`Topic View ${topicView.name}: ${topicView.specification} (${[...topicView.roles]})`);
        /// tag::log
        check.log(`Topic View ${topicView.name}: ${topicView.specification} (${[...topicView.roles]})`);
        /// end::log
    }

    await session.closeSession();
    /// end::topic_views_api_remove[]
    /// tag::log
    await check.done();
    /// end::log
}
