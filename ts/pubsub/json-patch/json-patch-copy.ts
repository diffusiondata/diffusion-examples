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
import { PartiallyOrderedCheckpointTester, promiseWithResolvers } from '../../../../test/util'
/// end::log

export async function jsonPatchCopyExample(): Promise<void> {
    /// tag::log
    const check = new PartiallyOrderedCheckpointTester([
        ['Subscribed to my/topic/path'],
        ['my/topic/path changed'],
        [
            'old value Meet the Flintstones/Fred: Flintstone',
            'old value Meet the Flintstones/Barney: Rubble',
            'old value The Jetsons/George: Jetson'
        ],
        [
            'new value Meet the Flintstones/Fred: Flintstone',
            'new value Meet the Flintstones/Barney: Rubble',
            'new value The Jetsons/George: Jetson',
            'new value The Jetsons/Fred: Flintstone'
        ],
        ['Closed'],
    ]);
    const valuePromise = promiseWithResolvers<void>();
    /// end::log
    /// tag::pub_sub_json_patch_copy[]
    // Connect to the server.
    const session = await connect({
        host: 'localhost',
        port: 8080,
        principal: 'admin',
        credentials: 'password'
    });

    const specification = new topics.TopicSpecification(topics.TopicType.JSON);
    const jsonData = datatypes.json().from({
        'Meet the Flintstones': {
            'Fred': 'Flintstone',
            'Barney': 'Rubble'
        },
        'The Jetsons': {
            'George': 'Jetson'
        }
    });
    await session.topicUpdate.set(
        'my/topic/path',
        datatypes.json(),
        jsonData,
        { specification: specification }
    );

    const valueStream = session.addStream('my/topic/path', datatypes.json());
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
                for (const name of Object.keys(oldJson[key])) {
                    check.log(`old value ${key}/${name}: ${oldJson[key][name]}`);
                }
            }
            for (const key of Object.keys(newJson)) {
                for (const name of Object.keys(newJson[key])) {
                    check.log(`new value ${key}/${name}: ${newJson[key][name]}`);
                }
            }
            valuePromise.resolve();
            /// end::log
        }
    });

    await session.select('my/topic/path');

    await session.topicUpdate.applyJsonPatch(
        'my/topic/path', [{
            'op': 'copy',
            'from': '/Meet the Flintstones/Fred',
            'path': '/The Jetsons/Fred'
        }]
    );

    /// tag::log
    await valuePromise.promise;
    /// end::log
    await session.closeSession();
    /// end::pub_sub_json_patch_copy[]
    /// tag::log
    await check.done();
    /// end::log
}
