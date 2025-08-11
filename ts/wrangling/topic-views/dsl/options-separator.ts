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

export async function topicViewsDslOptionsSeparator(): Promise<void> {
    /// tag::log
    const check = new PartiallyOrderedCheckpointTester([[
        'Subscribed to views/Fred_Flintstone',
        'Subscribed to views/Wilma_Flintstone',
        'Subscribed to views/Pebbles_Flintstone',
    ]]);
    /// end::log
    /// tag::topic_views_dsl_options_separator[]
    // Connect to the server.
    const session = await connect({
        host: 'localhost',
        port: 8080,
        principal: 'admin',
        credentials: 'password'
    });

    const specification = new topics.TopicSpecification(topics.TopicType.JSON);

    const jsonData1 = datatypes.json().from({
        name: 'Fred/Flintstone'
    });
    await session.topicUpdate.set(
        'my/topic/path/1',
        datatypes.json(),
        jsonData1,
        { specification: specification }
    );

    const jsonData2 = datatypes.json().from({
        name: 'Wilma/Flintstone'
    });
    await session.topicUpdate.set(
        'my/topic/path/2',
        datatypes.json(),
        jsonData2,
        { specification: specification }
    );

    const jsonData3 = datatypes.json().from({
        name: 'Pebbles/Flintstone'
    });
    await session.topicUpdate.set(
        'my/topic/path/3',
        datatypes.json(),
        jsonData3,
        { specification: specification }
    );

    const valueStream = session.addFallbackStream(datatypes.json());
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
        `map ?my/topic/path// to views/<scalar(/name)> separator '_'`
    );
    console.log(`Topic View ${topicView.name} has been created.`);

    await session.closeSession();
    /// end::topic_views_dsl_options_separator[]
    /// tag::log
    await expectJsonTopicToHaveValue('views/Fred_Flintstone', {
        name: 'Fred/Flintstone'
    });
    await expectJsonTopicToHaveValue('views/Wilma_Flintstone', {
        name: 'Wilma/Flintstone'
    });
    await expectJsonTopicToHaveValue('views/Pebbles_Flintstone', {
        name: 'Pebbles/Flintstone'
    });

    await check.done();
    /// end::log
}
