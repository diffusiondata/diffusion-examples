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

export async function topicViewsDslExpandValue() {
    /// tag::log
    const check = new PartiallyOrderedCheckpointTester([[
        'Subscribed to views/currency0',
        'Subscribed to views/currency1',
        'Subscribed to views/currency2',
        'Subscribed to views/currency3'
    ]]);
    /// end::log
    /// tag::topic_views_dsl_expand_value[]
    const session = await diffusion.connect({
        host: 'localhost',
        port: 8080,
        principal: 'admin',
        credentials: 'password'
    });

    const specification = new diffusion.topics.TopicSpecification(diffusion.topics.TopicType.JSON);
    const jsonData = diffusion.datatypes.json().from({
        'currencies': ['USD', 'GBP', 'EUR', 'CHF']
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
        'map my/topic/path to views/currency<expand(/currencies)>'
    );
    console.log(`Topic View ${topicView.name} has been created.`);

    await session.closeSession();
    /// end::topic_views_dsl_expand_value[]
    /// tag::log
    await expectJsonTopicToHaveValue('views/currency0', 'USD');
    await expectJsonTopicToHaveValue('views/currency1', 'GBP');
    await expectJsonTopicToHaveValue('views/currency2', 'EUR');
    await expectJsonTopicToHaveValue('views/currency3', 'CHF');

    await check.done();
    /// end::log
}
