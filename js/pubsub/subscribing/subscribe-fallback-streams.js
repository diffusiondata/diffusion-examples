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

export async function pubSubSubscribeFallbackStream() {
    /// tag::log
    const check = new PartiallyOrderedCheckpointTester([
        [
            'Subscribed to my/topic/path',
            'Subscribed to my/other/topic/path',
            'Subscribed to my/additional/topic/path'
        ],
        ['Closed'],
    ]);
    /// end::log
    /// tag::pub_sub_subscribe_fallback_stream[]
    // Connect to the server.
    const session = await diffusion.connect({
        host: 'localhost',
        port: 8080,
        principal: 'admin',
        credentials: 'password'
    });

    const specification = new diffusion.topics.TopicSpecification(diffusion.topics.TopicType.JSON);
    await session.topics.add('my/topic/path', specification);
    await session.topics.add('my/other/topic/path', specification);

    const valueStream = session.addFallbackStream(diffusion.datatypes.json());
    valueStream.on({
        subscribe : (topic, specification) => {
            console.log(`Subscribed to ${topic}`);
            /// tag::log
            check.log(`Subscribed to ${topic}`);
            /// end::log
        },
        unsubscribe : (topic, specification, reason) => {
            console.log(`Unsubscribed from ${topic}: ${reason}`);
            /// tag::log
            check.log(`Unsubscribed from ${topic}: ${reason}`);
            /// end::log
        },
        /// tag::log
        close : () => {
            check.log(`Closed`);
        },
        /// end::log
        value : (topic, spec, newValue, oldValue) => {
            console.log(`${topic} changed from ${oldValue.get()} to ${newValue.get()}`);
            /// tag::log
            check.log(`${topic} changed from ${oldValue.get()} to ${newValue.get()}`);
            /// end::log
        }
    });

    await session.select('?my//');

    console.log('Creating my/additional/topic/path');
    await session.topics.add('my/additional/topic/path', specification);
    console.log('Creating this/topic/path/will/not/be/picked/up');
    await session.topics.add('this/topic/path/will/not/be/picked/up', specification);

    await session.closeSession();
    /// end::pub_sub_subscribe_fallback_stream[]
    /// tag::log
    await check.done();
    /// end::log
}
