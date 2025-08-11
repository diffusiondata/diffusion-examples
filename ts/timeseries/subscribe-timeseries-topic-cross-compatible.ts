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
import { PartiallyOrderedCheckpointTester, promiseWithResolvers } from '../../../test/util'
/// end::log

export async function timeSeriesSubscribeCrossCompatible(): Promise<void> {
    /// tag::log
    const check = new PartiallyOrderedCheckpointTester([
        ['Subscribed to my/time/series/topic/path']
    ]);

    const valuePromise = promiseWithResolvers<void>();
    let valueCount = 0;
    let oldValue = 'undefined';
    /// end::log
    /// tag::time_series_subscribe_cross_compatible[]
    // Connect to the server.
    const session = await connect({
        host: 'localhost',
        port: 8080,
        principal: 'admin',
        credentials: 'password'
    });

    const specification = new topics.TopicSpecification(topics.TopicType.TIME_SERIES, {
        'TIME_SERIES_EVENT_VALUE_TYPE': 'double',
        'TIME_SERIES_RETAINED_RANGE': 'limit 15 last 10s',
        'TIME_SERIES_SUBSCRIPTION_RANGE': 'limit 3'
    });

    await session.topics.add('my/time/series/topic/path', specification);

    const jsonValueStream = session.addStream('my/time/series/topic/path', datatypes.json());
    jsonValueStream.on({
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
            console.log(`${topic} changed from ${oldValue?.get()} to ${newValue.get()}`);
            /// tag::log
            if (++valueCount >= 25) {
                valuePromise.resolve();
            }
            check.log(`${topic} changed from ${oldValue?.get()} to ${newValue.get()}`);
            /// end::log
        }
    });

    await session.select('?my/time/series//');

    for (let count = 0; count < 25; count++) {
        const value = Math.random();
        /// tag::log
        check.appendExpected([`my/time/series/topic/path changed from ${oldValue} to ${value}`]);
        oldValue = `${value}`;
        /// end::log
        await session.timeseries.append('my/time/series/topic/path', value, datatypes.double());
    }
    /// tag::log
    check.appendExpected([`Closed`]);
    await valuePromise.promise;
    /// end::log
    await session.closeSession();
    /// end::time_series_subscribe_cross_compatible[]
    /// tag::log
    await check.done();
    /// end::log
}
