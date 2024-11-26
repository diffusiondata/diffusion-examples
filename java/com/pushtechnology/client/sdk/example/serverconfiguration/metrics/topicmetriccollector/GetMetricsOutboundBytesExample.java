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

import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pushtechnology.diffusion.client.Diffusion;
import com.pushtechnology.diffusion.client.features.control.Metrics;
import com.pushtechnology.diffusion.client.session.Session;

public class GetMetricsOutboundBytesExample {

    private static final Logger LOG = LoggerFactory.getLogger(
        GetMetricsOutboundBytesExample.class);

    public static void main(String[] args) {

        Session session = Diffusion.sessions()
            .principal("admin")
            .password("password")
            .open("ws://localhost:8080");

        Metrics metricsControl = session.feature(Metrics.class);

        Metrics.MetricsResult result = metricsControl.metricsRequest()
            .currentServer()
            .filter(Collections.singleton("diffusion_network_outbound_bytes"))
            .fetch().join();

        String serverName = result.getServerNames().iterator().next();

        Metrics.MetricSampleCollection collection =
            result.getMetrics(serverName).get(0);

        Metrics.MetricSample sample = collection.getSamples().get(0);

        LOG.info("{}: {} {} ({})",
            collection.getName(),
            sample.getValue(),
            collection.getUnit(),
            collection.getType());

        session.close();
    }
}
