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

import { connect, datatypes, Session, topics } from 'diffusion';
/// tag::log
import { PartiallyOrderedCheckpointTester, promiseWithResolvers } from '../../../../test/util'
/// end::log

export async function pubSubSubscribeCrossCompatible(): Promise<void> {
    /// tag::log
    const check = new PartiallyOrderedCheckpointTester([
        [
            'JSON stream subscribed to my/int/topic/path',
            'String stream subscribed to my/int/topic/path'
        ],
        [
            'JSON stream closed',
            'String stream closed'
        ],
    ]);

    const { promise: promiseJSON, resolve: resolveJSON } = promiseWithResolvers<void>();
    const { promise: promiseString, resolve: resolveString } = promiseWithResolvers<void>();
    /// end::log
    /// tag::pub_sub_subscribe_cross_compatible[]
    // Connect to the server.
    const session = await connect({
        host: 'localhost',
        port: 8080,
        principal: 'admin',
        credentials: 'password'
    });

    const specification = new topics.TopicSpecification(topics.TopicType.INT64);
    await session.topics.add('my/int/topic/path', specification);

    const jsonValueStream = session.addStream('my/int/topic/path', datatypes.json());
    jsonValueStream.on({
        subscribe : (topic, specification) => {
            console.log(`JSON stream subscribed to ${topic}`);
            /// tag::log
            check.log(`JSON stream subscribed to ${topic}`);
            resolveJSON();
            /// end::log
        },
        unsubscribe : (topic, specification, reason) => {
            console.log(`JSON stream unsubscribed from ${topic}: ${reason}`, Date.now(), JSON.stringify(reason));
            /// tag::log
            check.log(`JSON stream unsubscribed from ${topic}: ${reason}`);
            /// end::log
        },
        /// tag::log
        close : () => {
            check.log(`JSON stream closed`);
        },
        /// end::log
        value : (topic, spec, newValue, oldValue) => {
            console.log(`JSON stream ${topic} changed from ${oldValue.get()} to ${newValue.get()}`);
            /// tag::log
            check.log(`JSON stream ${topic} changed from ${oldValue.get()} to ${newValue.get()}`);
            /// end::log
        }
    });

    const stringValueStream = session.addStream('my/int/topic/path', datatypes.string());
    stringValueStream.on({
        subscribe : (topic, specification) => {
            console.log(`String stream subscribed to ${topic}`);
            /// tag::log
            check.log(`String stream subscribed to ${topic}`);
            resolveString();
            /// end::log
        },
        unsubscribe : (topic, specification, reason) => {
            console.log(`String stream unsubscribed from ${topic}: ${reason}`);
            /// tag::log
            check.log(`String stream unsubscribed from ${topic}: ${reason}`);
            /// end::log
        },
        /// tag::log
        close : () => {
            check.log(`String stream closed`);
        },
        /// end::log
        value : (topic, spec, newValue, oldValue) => {
            console.log(`String stream ${topic} changed from ${oldValue} to ${newValue}`);
            /// tag::log
            check.log(`String stream ${topic} changed from ${oldValue} to ${newValue}`);
            /// end::log
        }
    });

    console.log('Selecting my/int/topic/path', Date.now());
    await session.select('my/int/topic/path');
    /// tag::log
    console.log('Waiting for both streams to subscribe', Date.now());
    await Promise.all([promiseJSON, promiseString]);
    /// end::log

    console.log('Closing session', Date.now());
    await session.closeSession();
    /// end::pub_sub_subscribe_cross_compatible[]
    /// tag::log
    await check.done();
    /// end::log
}
