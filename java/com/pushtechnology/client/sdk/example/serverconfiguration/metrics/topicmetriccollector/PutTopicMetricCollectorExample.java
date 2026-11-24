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
package com.pushtechnology.client.sdk.example.serverconfiguration.metrics.topicmetriccollector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pushtechnology.diffusion.client.Diffusion;
import com.pushtechnology.diffusion.client.features.control.Metrics;
import com.pushtechnology.diffusion.client.features.control.Metrics.TopicMetricCollector;
import com.pushtechnology.diffusion.client.session.Session;

/**
 * This example demonstrates how to add metric collectors using the Metrics
 * feature in Diffusion.
 *
 * @author DiffusionData Limited
 */
public class PutTopicMetricCollectorExample {

    private static final Logger LOG = LoggerFactory.getLogger(
        PutTopicMetricCollectorExample.class);

    public static void main(String[] args) {

        final Session session = Diffusion.sessions()
            .principal("admin")
            .password("password")
            .open("ws://localhost:8080");

        final Metrics metricsControl = session.feature(Metrics.class);

        final TopicMetricCollector topicMetricCollector =
            Diffusion.newTopicMetricCollectorBuilder()
                .exportToPrometheus(false)
                .maximumGroups(10)
                .groupByTopicType(true)
                .groupByTopicView(false)
                .create("Topic Metric Collector 1", "?/my/topic//");

        metricsControl.putTopicMetricCollector(topicMetricCollector).join();

        System.out.printf("Created %s\n", topicMetricCollector.getName());
        session.close();

        LOG.info("Created {}", topicMetricCollector.getName());
    }
}
