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

export async function pubSubFetchTopicProperties() {
    // Connect to the server.
    const session = await diffusion.connect({
        host: 'localhost',
        port: 8080,
        principal: 'admin',
        credentials: 'password'
    });


    const specification1 = new diffusion.topics.TopicSpecification(diffusion.topics.TopicType.JSON, {
        'DONT_RETAIN_VALUE': 'true',
        'PERSISTENT': 'false',
        'PUBLISH_VALUES_ONLY': 'true'
    });
    for (let count = 0; count < 5; count++) {
        const jsonData = diffusion.datatypes.json().from({ diffusion: `data #${count}` });
        await session.topicUpdate.set(
            `my/topic/path/with/properties/${count}`,
            diffusion.datatypes.json(),
            jsonData,
            { specification: specification1 }
        );
    }

    const specification2 = new diffusion.topics.TopicSpecification(diffusion.topics.TopicType.STRING);
    for (let count = 0; count < 5; count++) {
        await session.topicUpdate.set(
            `my/topic/path/with/default/properties/${count}`,
            diffusion.datatypes.string(),
            `diffusion data #${count}`,
            { specification: specification2 }
        );
    }

    const fetchResult1 = await session.fetchRequest()
        .withProperties()
        .topicTypes([ diffusion.topics.TopicType.JSON ])
        .fetch('?my/topic/path//');
    for (const result of fetchResult1.results()) {
        console.log(`${result.path()}  properties:`);
        const properties = result.specification().properties;
        for (const property of Object.keys(properties)) {
            console.log(`  ${property}: ${properties[property]}`);
        }
    }

    const fetchResult2 = await session.fetchRequest()
        .withProperties()
        .topicTypes([ diffusion.topics.TopicType.STRING ])
        .fetch('?my/topic/path//');
    for (const result of fetchResult2.results()) {
        console.log(`${result.path}  properties:`);
        const properties = result.specification().properties;
        for (const property of Object.keys(properties)) {
            console.log(`  ${property}: ${properties[property]}`);
        }
    }

    await session.closeSession();
}
