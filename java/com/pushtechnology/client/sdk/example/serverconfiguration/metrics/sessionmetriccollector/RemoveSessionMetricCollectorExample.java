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
import com.pushtechnology.diffusion.client.session.Session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemoveSessionMetricCollectorExample {

    private static final Logger LOG = LoggerFactory.getLogger(
        RemoveSessionMetricCollectorExample.class);

    public static void main(String[] args) throws Exception {

        Session adminSession = Diffusion.sessions()
            .principal("admin")
            .password("password")
            .open("ws://localhost:8080");

        Metrics metricsControl = adminSession.feature(Metrics.class);

        Metrics.SessionMetricCollector sessionMetricCollector =
            Diffusion.newSessionMetricCollectorBuilder()
                .exportToPrometheus(false)
                .maximumGroups(10)
                .removeMetricsWithNoMatches(true)
                .groupByProperty("$Location")
                .create("Session Metric Collector 1", "$Principal is 'control'");

        metricsControl.putSessionMetricCollector(sessionMetricCollector);
        SECONDS.sleep(5);

        metricsControl.removeSessionMetricCollector(sessionMetricCollector.getName());
        System.out.printf("%s has been removed", sessionMetricCollector.getName());

        adminSession.close();

        LOG.info("{} has been removed", sessionMetricCollector.getName());
    }
}
