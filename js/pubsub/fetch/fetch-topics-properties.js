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
const { PartiallyOrderedCheckpointTester } = require('../../../../test/util');
/// end::log

export async function pubSubFetchTopicProperties() {
    /// tag::log
    const check = new PartiallyOrderedCheckpointTester([
        [
            'my/topic/path/with/properties/0 DONT_RETAIN_VALUE: true',
            'my/topic/path/with/properties/0 PERSISTENT: false',
            'my/topic/path/with/properties/0 PUBLISH_VALUES_ONLY: true',
            'my/topic/path/with/properties/1 DONT_RETAIN_VALUE: true',
            'my/topic/path/with/properties/1 PERSISTENT: false',
            'my/topic/path/with/properties/1 PUBLISH_VALUES_ONLY: true',
            'my/topic/path/with/properties/2 DONT_RETAIN_VALUE: true',
            'my/topic/path/with/properties/2 PERSISTENT: false',
            'my/topic/path/with/properties/2 PUBLISH_VALUES_ONLY: true',
            'my/topic/path/with/properties/3 DONT_RETAIN_VALUE: true',
            'my/topic/path/with/properties/3 PERSISTENT: false',
            'my/topic/path/with/properties/3 PUBLISH_VALUES_ONLY: true',
            'my/topic/path/with/properties/4 DONT_RETAIN_VALUE: true',
            'my/topic/path/with/properties/4 PERSISTENT: false',
            'my/topic/path/with/properties/4 PUBLISH_VALUES_ONLY: true'
        ]
    ]);
    /// end::log
    /// tag::pub_sub_fetch_topic_properties[]
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
            /// tag::log
            if (['DONT_RETAIN_VALUE', 'PERSISTENT', 'PUBLISH_VALUES_ONLY'].includes(property)) {
                check.log(`${result.path()} ${property}: ${properties[property]}`);
            }
            /// end::log
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
            /// tag::log
            if (['DONT_RETAIN_VALUE', 'PERSISTENT', 'PUBLISH_VALUES_ONLY'].includes(property)) {
                check.log(`${result.path()} ${property}: ${properties[property]}`);
            }
            /// end::log
        }
    }

    await session.closeSession();
    /// end::pub_sub_fetch_topic_properties[]
}
