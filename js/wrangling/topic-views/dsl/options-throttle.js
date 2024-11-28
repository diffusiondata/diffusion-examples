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
const { PartiallyOrderedCheckpointTester } = require('../../../../../test/util');
/// end::log

export async function topicViewsDslOptionsThrottle() {
    /// tag::log
    const check = new PartiallyOrderedCheckpointTester([
        [ 'Subscribed to views/my/topic/path', 'Subscribed to my/topic/path' ],
        [
            'views/my/topic/path changed from undefined',
            'views/my/topic/path changed from 0'
        ],
    ]);
    /// end::log
    /// tag::topic_views_dsl_options_throttle[]
    const session = await diffusion.connect({
        host: 'localhost',
        port: 8080,
        principal: 'admin',
        credentials: 'password'
    });

    const specification = new diffusion.topics.TopicSpecification(diffusion.topics.TopicType.INT64);
    await session.topicUpdate.set(
        'my/topic/path',
        diffusion.datatypes.int64(),
        0,
        { specification: specification }
    );

    const valueStream = session.addFallbackStream(diffusion.datatypes.int64());
    valueStream.on({
        subscribe : (topic, specification) => {
            console.log(`Subscribed to ${topic}`);
            /// tag::log
            check.log(`Subscribed to ${topic}`);
            /// end::log
        },
        unsubscribe : (topic, specification, reason) => {
            console.log(`Unsubscribed from ${topic}: ${reason}`);
        },
        value : (topic, spec, newValue, oldValue) => {
            console.log(`${topic} changed from ${oldValue} to ${newValue}`);
            /// tag::log
            if (topic !== 'my/topic/path') {
                check.log(`${topic} changed from ${oldValue}`);
            }
            /// end::log
        }
    });

    await session.select('?.*//');

    const topicView = await session.topicViews.createTopicView(
        'topic_view_1',
        'map my/topic/path to views/<path(0)> throttle to 1 update every 3 seconds'
    );
    console.log(`Topic View ${topicView.name} has been created.`);

    for (let i = 0; i < 15; i++) {
        await session.topicUpdate.set(
            'my/topic/path',
            diffusion.datatypes.int64(),
            Date.now(),
            { specification: specification }
        );
        await new Promise((resolve) => setTimeout(resolve, 1000));
    }

    await session.closeSession();
    /// end::topic_views_dsl_options_throttle[]
    /// tag::log
    await check.done();
    /// end::log
}
