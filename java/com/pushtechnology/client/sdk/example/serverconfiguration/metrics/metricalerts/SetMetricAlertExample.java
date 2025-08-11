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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pushtechnology.diffusion.client.Diffusion;
import com.pushtechnology.diffusion.client.features.Topics;
import com.pushtechnology.diffusion.client.features.control.Metrics;
import com.pushtechnology.diffusion.client.session.Session;
import com.pushtechnology.diffusion.datatype.json.JSON;

/**
 * This example demonstrates how to set metric alerts in Diffusion.
 *
 * @author DiffusionData Limited
 */
public class SetMetricAlertExample {

    private static final Logger LOG = LoggerFactory.getLogger(
        SetMetricAlertExample.class);

    public static void main(String[] args) throws Exception  {

        final Session session = Diffusion.sessions()
            .principal("admin")
            .password("password")
            .open("ws://localhost:8080");

        final Metrics metrics = session.feature(Metrics.class);

        metrics.setMetricAlert("myAlert", "select os_system_cpu_load into topic my/topic/path").join();

        LOG.info("alert created");
        Thread.sleep(5000);

        final Topics.FetchResult<JSON> result = session.feature(Topics.class)
            .fetchRequest().withValues(JSON.class).fetch("my/topic/path").join();

        final String topicValue = result.results().get(0).value().toJsonString();

        LOG.info("Topic value: {}", topicValue);
        session.close();
    }
}