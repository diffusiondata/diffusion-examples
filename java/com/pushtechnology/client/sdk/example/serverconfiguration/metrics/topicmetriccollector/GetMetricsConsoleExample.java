/*******************************************************************************
 * Copyright (C) 2024 DiffusionData Ltd.
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
package com.pushtechnology.client.sdk.example.serverconfiguration.metrics.topicmetriccollector;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pushtechnology.diffusion.client.Diffusion;
import com.pushtechnology.diffusion.client.features.control.Metrics;
import com.pushtechnology.diffusion.client.session.Session;

public class GetMetricsConsoleExample {

    private static final Logger LOG = LoggerFactory.getLogger(
        GetMetricsConsoleExample.class);

    public static void main(String[] args) {

        Session session = Diffusion.sessions()
            .principal("admin")
            .password("password")
            .open("ws://localhost:8080");

        Metrics metricsControl = session.feature(Metrics.class);

        Set<String> requiredMetrics = new HashSet<>(
            Arrays.asList(
                "diffusion_server_time_zone",
                "diffusion_server_user_directory",
                "diffusion_server_license_expiry_date",
                "diffusion_server_uptime_millis",
                "diffusion_license",
                "diffusion_server_free_memory_bytes",
                "diffusion_server_max_memory_bytes",
                "diffusion_server_total_memory_bytes",
                "diffusion_release",
                "diffusion_server_start_date",
                "diffusion_server_start_date_millis",
                "diffusion_server_used_swap_space_size_bytes",
                "diffusion_server_used_physical_memory_size_bytes",
                "diffusion_server_number_of_topics",
                "diffusion_server_session_locks",
                "diffusion_server_user_name",
                "diffusion_server_uptime",
                "diffusion_multiplexer_manager_number_of_multiplexers"
            )
        );

        Metrics.MetricsResult result = metricsControl.metricsRequest()
            .filter(requiredMetrics)
            .fetch().join();

        String serverName = result.getServerNames().iterator().next();

        List<Metrics.MetricSampleCollection> collections =
            result.getMetrics(serverName);

        collections.forEach(collection -> {
            collection.getSamples().forEach(metricSample -> {

                String name = metricSample.getName();
                String type = collection.getType().name();
                String unit = collection.getUnit();

                String value = "GAUGE".equals(type) ?
                    String.valueOf(metricSample.getValue()) :
                    metricSample.getLabelValues().toString();

                LOG.info("{}: {} {} ({})", name, value, unit, type);
            });
        });

        session.close();
    }
}