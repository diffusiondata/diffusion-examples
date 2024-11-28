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
const { expectTimeseriesDoubleTopicToHaveValues } = require('../../../test/util');
/// end::log

export async function timeSeriesAppendUserSuppliedTimestamp() {
    /// tag::log
    const values = [];
    /// end::log
    /// tag::time_series_append_user_supplied_timestamp[]
    // Connect to the server.
    const session = await diffusion.connect({
        host: 'localhost',
        port: 8080,
        principal: 'admin',
        credentials: 'password'
    });

    const specification = new diffusion.topics.TopicSpecification(diffusion.topics.TopicType.TIME_SERIES, {
        'TIME_SERIES_EVENT_VALUE_TYPE': 'double',
        'TIME_SERIES_RETAINED_RANGE': 'limit 15 last 10s',
        'TIME_SERIES_SUBSCRIPTION_RANGE': 'limit 3'
    });

    await session.topics.add('my/time/series/topic/path/user/supplied', specification);

    let timestamp = 1000001;
    for (let count = 0; count < 25; count++) {
        const value = Math.random();
        await session.timeseries.append(
            'my/time/series/topic/path/user/supplied',
            value,
            diffusion.datatypes.double(),
            timestamp
        );
        /// tag::log
        values.push(value);
        /// end::log
        timestamp++;
    }

    await session.closeSession();
    /// end::time_series_append_user_supplied_timestamp[]
    /// tag::log
    const timestamps = values.map((_, i) => 1000001 + i);
    await expectTimeseriesDoubleTopicToHaveValues(
        'my/time/series/topic/path/user/supplied',
        values.slice(-15),
        timestamps.slice(-15)
    );
    /// end::log
}
