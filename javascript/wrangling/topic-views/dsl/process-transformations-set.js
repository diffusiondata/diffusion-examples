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

export async function topicViewsDslProcessTransformationsSet() {
    const session = await diffusion.connect({
        host: 'localhost',
        port: 8080,
        principal: 'admin',
        credentials: 'password'
    });

    const specification = new diffusion.topics.TopicSpecification(diffusion.topics.TopicType.JSON);
    const jsonData1 = diffusion.datatypes.json().from({
        account: '1234',
        balance: {
            amount: 12.57,
            currency: 'USD'
        }
    });
    await session.topicUpdate.set(
        'my/topic/path/1',
        diffusion.datatypes.json(),
        jsonData1,
        { specification: specification }
    );

    const jsonData2 = diffusion.datatypes.json().from({
        account: '5678',
        balance: {
            amount: 98.76,
            currency: 'USD'
        }
    });
    await session.topicUpdate.set(
        'my/topic/path/2',
        diffusion.datatypes.json(),
        jsonData2,
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

    const topicView = await session.topicViews.createTopicView(
        'topic_view_1',
        `map ?my/topic/path// to views/<path(3)>
         process {
            if '/balance/amount > 20'
                set(/tier, 1)
            else
                set(/tier, 2)
        }
        process {
            set(/balance/amount_in_cents, calc '/balance/amount * 100')
        }`
    );
    console.log(`Topic View ${topicView.name} has been created.`);

    await session.closeSession();
}
