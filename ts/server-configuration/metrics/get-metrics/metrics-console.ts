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

import { connect, MetricType } from 'diffusion';
/// tag::log
import { PartiallyOrderedCheckpointTester } from '../../../../../test/util';
/// end::log

export async function metricsRequestMetricsConsole(): Promise<void> {
    /// tag::log
    const check = new PartiallyOrderedCheckpointTester([
        [
            'diffusion_license: diffusion_license  (INFO)',
            'diffusion_multiplexer_manager_number_of_multiplexers: diffusion_multiplexer_manager_number_of_multiplexers  (GAUGE)',
            'diffusion_release: diffusion_release  (INFO)',
            'diffusion_server_free_memory_bytes: diffusion_server_free_memory_bytes bytes (GAUGE)',
            'diffusion_server_license_expiry_date: diffusion_server_license_expiry_date  (INFO)',
            'diffusion_server_max_memory_bytes: diffusion_server_max_memory_bytes bytes (GAUGE)',
            'diffusion_server_number_of_topics: diffusion_server_number_of_topics  (GAUGE)',
            'diffusion_server_session_locks: diffusion_server_session_locks  (INFO)',
            'diffusion_server_start_date: diffusion_server_start_date  (INFO)',
            'diffusion_server_start_date_millis: diffusion_server_start_date_millis millis (GAUGE)',
            'diffusion_server_time_zone: diffusion_server_time_zone  (INFO)',
            'diffusion_server_total_memory_bytes: diffusion_server_total_memory_bytes bytes (GAUGE)',
            'diffusion_server_uptime: diffusion_server_uptime  (INFO)',
            'diffusion_server_uptime_millis: diffusion_server_uptime_millis millis (GAUGE)',
            'diffusion_server_used_physical_memory_size_bytes: diffusion_server_used_physical_memory_size_bytes bytes (GAUGE)',
            'diffusion_server_used_swap_space_size_bytes: diffusion_server_used_swap_space_size_bytes bytes (GAUGE)',
            'diffusion_server_user_directory: diffusion_server_user_directory  (INFO)',
            'diffusion_server_user_name: diffusion_server_user_name  (INFO)'
        ],
    ]);
    /// end::log
    /// tag::metrics_get_metrics_console[]
    // Connect to the server.
    const session = await connect({
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
    /// tag::log
    expect(servers.size).toBe(1);
    /// end::log
    const collections = metricsResult.getMetrics([...servers.values()][0]);
    for (const collection of collections) {
        for (const sample of collection.samples) {
            console.log(`${collection.name}: ${sample.value} ${collection.unit} (${MetricType[collection.type]})`);
            /// tag::log
            check.log(`${collection.name}: ${sample.name} ${collection.unit} (${MetricType[collection.type]})`);
            /// end::log
        }
    }

    await session.closeSession();
    /// end::metrics_get_metrics_console[]
}
