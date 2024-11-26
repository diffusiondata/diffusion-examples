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

export async function topicViewsDslSourcePathDirective(): Promise<void> {
    // Connect to the server.
    const session = await connect({
        host: 'localhost',
        port: 8080,
        principal: 'admin',
        credentials: 'password'
    });

    const specification = new topics.TopicSpecification(topics.TopicType.JSON);
    const jsonData = datatypes.json().from({
        account: '1234',
        balance: {
            amount: 12.57,
            currency: 'USD'
        }
    });
    await session.topicUpdate.set(
        'a/b/c/d/e/f/g',
        datatypes.json(),
        jsonData,
        { specification: specification }
    );

    const valueStream = session.addFallbackStream(datatypes.json());
    valueStream.on({
        subscribe : (topic, specification) => {
            console.log(`Subscribed to ${topic}`);
        },
        unsubscribe : (topic, specification, reason) => {},
        value : (topic, spec, newValue, oldValue) => {}
    });

    await session.select('?views//');

    const topicView1 = await session.topicViews.createTopicView(
        'topic_view_1',
        'map a/b/c/d/e/f/g to views/<path(0)>'
    );
    console.log(`Topic View ${topicView1.name} has been created.`);
    const topicView2 = await session.topicViews.createTopicView(
        'topic_view_2',
        'map a/b/c/d/e/f/g to views/<path(2)>'
    );
    console.log(`Topic View ${topicView2.name} has been created.`);
    const topicView3 = await session.topicViews.createTopicView(
        'topic_view_3',
        'map a/b/c/d/e/f/g to views/<path(3,5)>'
    );
    console.log(`Topic View ${topicView3.name} has been created.`);

    await session.closeSession();
}
