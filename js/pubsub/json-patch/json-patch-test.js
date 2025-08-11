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

export async function jsonPatchTestExample() {
    /// tag::log
    const check = new PartiallyOrderedCheckpointTester([
        ['Subscribed to my/topic/path'],
        ['Closed'],
    ]);
    /// end::log
    /// tag::pub_sub_json_patch_test[]
    // Connect to the server.
    const session = await diffusion.connect({
        host: 'localhost',
        port: 8080,
        principal: 'admin',
        credentials: 'password'
    });

    const specification = new diffusion.topics.TopicSpecification(diffusion.topics.TopicType.JSON);
    const jsonData = diffusion.datatypes.json().from({
        Fred: 'Flintstone',
        Barney: 'Rubble',
        George: 'Jetson'
    });
    await session.topicUpdate.set(
        'my/topic/path',
        diffusion.datatypes.json(),
        jsonData,
        { specification: specification }
    );

    const valueStream = session.addStream('my/topic/path', diffusion.datatypes.json());
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
        /// tag::log
        close : () => {
            check.log(`Closed`);
        },
        /// end::log
        value : (topic, spec, newValue, oldValue) => {
            console.log(`${topic} changed from ${JSON.stringify(oldValue.get())} to ${JSON.stringify(newValue.get())}`);
            /// tag::log
            check.log(`${topic} changed`);
            const oldJson = oldValue.get();
            const newJson = newValue.get();
            for (const key of Object.keys(oldJson)) {
                check.log(`old value ${key}: ${oldJson[key]}`);
            }
            for (const key of Object.keys(newJson)) {
                check.log(`new value ${key}: ${newJson[key]}`);
            }
            /// end::log
        }
    });

    await session.select('my/topic/path');

    await session.topicUpdate.applyJsonPatch(
        'my/topic/path', [{
            op: 'test',
            path: '/Fred',
            value: 'Flintstone'
        }]
    );

    await session.closeSession();
    /// end::pub_sub_json_patch_test[]
    /// tag::log
    await check.done();
    /// end::log
}

