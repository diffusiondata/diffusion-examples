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

import { connect, topics } from 'diffusion';
/// tag::log
import { expectTopicExists } from '../../../../test/util';
/// end::log

export async function addTopicWithPropertiesExample(): Promise<void> {
    /// tag::pub_sub_publish_add_topic_custom_properties[]
    // Connect to the server.
    const session = await connect({
        host: 'localhost',
        port: 8080,
        principal: 'admin',
        credentials: 'password'
    });

    const specification = new topics.TopicSpecification(topics.TopicType.JSON, {
        'DONT_RETAIN_VALUE': 'true',
        'PERSISTENT': 'false',
        'PUBLISH_VALUES_ONLY': 'true'
    });
    const topicAddResult = await session.topics.add('my/topic/path', specification);
    if (topicAddResult.added) {
        console.log('Topic has been created.');
    } else {
        console.log('Topic already exists.');
    }
    /// tag::log
    expect(topicAddResult.added).toBe(true);
    /// end::log

    await session.closeSession();
    /// end::pub_sub_publish_add_topic_custom_properties[]
    /// tag::log
    await expectTopicExists('my/topic/path');
    /// end::log
}
