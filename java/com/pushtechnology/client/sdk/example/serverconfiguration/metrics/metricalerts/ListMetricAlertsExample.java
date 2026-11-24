/*******************************************************************************
 * Copyright (C) 2025 DiffusionData Ltd.
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
package com.pushtechnology.client.sdk.example.serverconfiguration.metrics.metricalerts;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pushtechnology.diffusion.client.Diffusion;
import com.pushtechnology.diffusion.client.features.control.Metrics;
import com.pushtechnology.diffusion.client.session.Session;

/**
 * This example demonstrates how to list metric alerts in Diffusion.
 *
 * @author DiffusionData Limited
 */
public class ListMetricAlertsExample {

    private static final Logger LOG = LoggerFactory.getLogger(
        ListMetricAlertsExample.class);

    public static void main(String[] args) throws Exception {

        final Session session = Diffusion.sessions()
            .principal("admin")
            .password("password")
            .open("ws://localhost:8080");

        final Metrics metrics = session.feature(Metrics.class);

        for (int i = 0; i < 5; i++) {
            final String alertName = "alert" + i;
            final String topic = "my/topic/path/" + i;

            metrics.setMetricAlert(alertName,
                "select os_system_cpu_load into topic " + topic).join();
        }

        final List<Metrics.MetricAlert> alerts = metrics.listMetricAlerts().join();
        alerts.forEach(alert -> LOG.info(alert.getName()));

        session.close();
    }
}