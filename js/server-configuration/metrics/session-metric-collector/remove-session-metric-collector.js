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

export async function metricsRemoveSessionMetricCollector() {
    // Connect to the server.
    const session = await diffusion.connect({
        host: 'localhost',
        port: 8080,
        principal: 'admin',
        credentials: 'password'
    });

    const sessionMetricCollector = diffusion.newSessionMetricCollectorBuilder()
        .exportToPrometheus(false)
        .maximumGroups(10)
        .removeMetricsWithNoMatches(true)
        .groupByProperty('$Location')
        .create('Session Metric Collector 1', '$Principal is "control"');
    await session.metrics.putSessionMetricCollector(sessionMetricCollector);

    await session.metrics.removeSessionMetricCollector('Session Metric Collector 1');

    await session.closeSession();
}
