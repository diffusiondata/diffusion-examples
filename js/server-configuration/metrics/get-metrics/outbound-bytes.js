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

export async function metricsGetOutboundBytes() {
    /// tag::metrics_get_metrics_outbound_bytes[]
    // Connect to the server.
    const session = await diffusion.connect({
        host: 'localhost',
        port: 8080,
        principal: 'admin',
        credentials: 'password'
    });

    const metricsResult = await session.metrics.metricsRequest()
        .filter('diffusion_network_outbound_bytes')
        .currentServer()
        .fetch();

    const servers = metricsResult.getServerNames();
    /// tag::log
    expect(servers.size).toBe(1);
    /// end::log
    const collections = metricsResult.getMetrics([...servers.values()][0]);
    /// tag::log
    expect(collections[0].samples.length).toBe(1);
    /// end::log
    const sample = collections[0].samples[0];
    console.log(`${collections[0].name}: ${sample.value} ${collections[0].unit} (${collections[0].type})`);

    await session.closeSession();
    /// end::metrics_get_metrics_outbound_bytes[]
}
