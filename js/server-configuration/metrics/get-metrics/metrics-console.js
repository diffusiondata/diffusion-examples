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

export async function metricsRequestMetricsConsole() {
    // Connect to the server.
    const session = await diffusion.connect({
        host: 'localhost',
        port: 8080,
        principal: 'admin',
        credentials: 'password'
    });

    const metricsResult = await session.metrics.metricsRequest()
        .filter([
            'diffusion_server_time_zone',
            'diffusion_server_user_directory',
            'diffusion_server_license_expiry_date',
            'diffusion_server_uptime_millis',
            'diffusion_license',
            'diffusion_server_free_memory_bytes',
            'diffusion_server_max_memory_bytes',
            'diffusion_server_total_memory_bytes',
            'diffusion_release',
            'diffusion_server_start_date',
            'diffusion_server_start_date_millis',
            'diffusion_server_used_swap_space_size_bytes',
            'diffusion_server_used_physical_memory_size_bytes',
            'diffusion_server_number_of_topics',
            'diffusion_server_session_locks',
            'diffusion_server_user_name',
            'diffusion_server_uptime',
            'diffusion_multiplexer_manager_number_of_multiplexers'
        ])
        .currentServer()
        .fetch();

    const servers = metricsResult.getServerNames();
    const collections = metricsResult.getMetrics([...servers.values()][0]);
    for (const collection of collections) {
        for (const sample of collection.samples) {
            console.log(`${collection.name}: ${sample.value} ${collection.unit} (${diffusion.MetricType[collection.type]})`);
        }
    }

    await session.closeSession();
}
