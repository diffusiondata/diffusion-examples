/*******************************************************************************
 * Copyright (C) 2023 - 2024 DiffusionData Ltd.
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
package com.pushtechnology.client.sdk.example.serverconfiguration.metrics.sessionmetriccollector;

import static java.util.concurrent.TimeUnit.SECONDS;

import com.pushtechnology.diffusion.client.Diffusion;
import com.pushtechnology.diffusion.client.features.control.Metrics;
import com.pushtechnology.diffusion.client.features.control.Metrics.SessionMetricCollector;
import com.pushtechnology.diffusion.client.session.Session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ListSessionMetricCollectorsExample {

    private static final Logger LOG = LoggerFactory.getLogger(
        ListSessionMetricCollectorsExample.class);

    public static void main(String[] args) throws Exception {

        Session session = Diffusion.sessions()
            .principal("admin")
            .password("password")
            .open("ws://localhost:8080");

        Metrics metricsControl = session.feature(Metrics.class);
        SessionMetricCollector.Builder builder = Diffusion.newSessionMetricCollectorBuilder();

        metricsControl.putSessionMetricCollector(builder
            .exportToPrometheus(false)
            .maximumGroups(10)
            .removeMetricsWithNoMatches(true)
            .groupByProperty("$Location")
            .create("Session Metric Collector 1", "$Principal is 'control'"));

        builder.reset();

        metricsControl.putSessionMetricCollector(builder
            .exportToPrometheus(true)
            .maximumGroups(250)
            .removeMetricsWithNoMatches(true)
            .groupByProperty("$Location")
            .create("Session Metric Collector 2", "$Principal is 'control'"));

        List<SessionMetricCollector> collectors = metricsControl
            .listSessionMetricCollectors()
            .join();

        collectors.forEach(c ->
            System.out.printf("<%s>: <%s> (<%d>, <%b>,<%b>,<%s>)\n",
                c.getName(),
                c.getSessionFilter(),
                c.maximumGroups(),
                c.exportsToPrometheus(),
                c.removesMetricsWithNoMatches(),
                c.getGroupByProperties())
        );

        SECONDS.sleep(5);
        session.close();

        LOG.info("Listed {} collectors", collectors.size());
    }
}
