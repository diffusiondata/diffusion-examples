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

export async function metricsListSessionMetricCollectors() {
    /// tag::metrics_session_metric_collector_list[]
    // Connect to the server.
    const session = await diffusion.connect({
        host: 'localhost',
        port: 8080,
        principal: 'admin',
        credentials: 'password'
    });

    const sessionMetricCollector1 = diffusion.newSessionMetricCollectorBuilder()
        .exportToPrometheus(false)
        .maximumGroups(10)
        .removeMetricsWithNoMatches(true)
        .groupByProperty('$Location')
        .create('Session Metric Collector 1', '$Principal is "control"');
    const sessionMetricCollector2 = diffusion.newSessionMetricCollectorBuilder()
        .exportToPrometheus(true)
        .maximumGroups(250)
        .removeMetricsWithNoMatches(true)
        .groupByProperty('$Location')
        .create('Session Metric Collector 2', '$Principal is "control"');
    await Promise.all([
        session.metrics.putSessionMetricCollector(sessionMetricCollector1),
        session.metrics.putSessionMetricCollector(sessionMetricCollector2)
    ]);

    const sessionMetricCollectors = await session.metrics.listSessionMetricCollectors();
    for (const collector of sessionMetricCollectors.collectors) {
        console.log(`${collector.name}: ${collector.sessionFilter} (${collector.maximumGroups}, ` +
            `${collector.exportToPrometheus}, ${collector.removeMetricsWithNoMatches}, ` +
            `[${collector.groupByProperties.join(', ')}])`);
    }
    /// tag::log
    expect(sessionMetricCollectors.collectors.length).toBe(2);
    expect(sessionMetricCollectors.collectors.map((c) => c.name)).toEqual(jasmine.arrayContaining(
        ['Session Metric Collector 1', 'Session Metric Collector 2']
    ));
    /// end::log

    await session.closeSession();
    /// end::metrics_session_metric_collector_list[]
}
