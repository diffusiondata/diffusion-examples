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

export async function topicViewsDslOptionsPreserveTopics() {
    const session = await diffusion.connect({
        host: 'localhost',
        port: 8080,
        principal: 'admin',
        credentials: 'password'
    });

    const specification = new diffusion.topics.TopicSpecification(diffusion.topics.TopicType.JSON);
    const jsonData1 = diffusion.datatypes.json().from({
        name: 'Fred Flintstone'
    });
    await session.topicUpdate.set(
        'my/topic/path',
        diffusion.datatypes.json(),
        jsonData1,
        { specification: specification }
    );

    const valueStream = session.addFallbackStream(diffusion.datatypes.json());
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
        'map my/topic/path to views/preserved/<scalar(/name)> preserve topics'
    );
    console.log(`Topic View ${topicView1.name} has been created.`);
    const topicView2 = await session.topicViews.createTopicView(
        'topic_view_2',
        'map my/topic/path to views/not_preserved/<scalar(/name)>'
    );
    console.log(`Topic View ${topicView2.name} has been created.`);

    await new Promise(resolve => setTimeout(resolve, 2000));

    const jsonData2 = diffusion.datatypes.json().from({
        name: 'Wilma Flintstone'
    });
    await session.topicUpdate.set(
        'my/topic/path',
        diffusion.datatypes.json(),
        jsonData2,
    );

    await new Promise(resolve => setTimeout(resolve, 2000));

    const jsonData3 = diffusion.datatypes.json().from({
        name: 'Pebbles Flintstone'
    });
    await session.topicUpdate.set(
        'my/topic/path',
        diffusion.datatypes.json(),
        jsonData3,
    );

    await new Promise(resolve => setTimeout(resolve, 2000));

    await session.topics.remove('my/topic/path');

    await session.closeSession();
}
