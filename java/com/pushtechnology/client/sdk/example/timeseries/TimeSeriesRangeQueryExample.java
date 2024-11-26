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
package com.pushtechnology.client.sdk.example.timeseries;

import static com.pushtechnology.diffusion.datatype.DataTypes.DOUBLE_DATATYPE_NAME;

import com.pushtechnology.diffusion.client.Diffusion;
import com.pushtechnology.diffusion.client.features.TimeSeries;
import com.pushtechnology.diffusion.client.features.control.topics.TopicControl;
import com.pushtechnology.diffusion.client.session.Session;
import com.pushtechnology.diffusion.client.topics.details.TopicSpecification;
import com.pushtechnology.diffusion.client.topics.details.TopicType;
import com.pushtechnology.diffusion.datatype.Bytes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;

public class TimeSeriesRangeQueryExample {
    private static final Logger LOG = LoggerFactory.getLogger(TimeSeriesRangeQueryExample.class);

    public static void main(String[] args) {
        try (Session session = Diffusion.sessions()
            .principal("admin")
            .password("password")
            .open("ws://localhost:8080")) {

            final String topicPath = "my/time/series/topic/path";

            session.feature(TopicControl.class).addTopic(
                    topicPath, Diffusion.newTopicSpecification(TopicType.TIME_SERIES)
                        .withProperty(TopicSpecification.TIME_SERIES_EVENT_VALUE_TYPE, DOUBLE_DATATYPE_NAME)
                        .withProperty(TopicSpecification.TIME_SERIES_RETAINED_RANGE, "limit 20 last 120s")
                        .withProperty(TopicSpecification.TIME_SERIES_SUBSCRIPTION_RANGE, "limit 3"))
                .join();

            final TimeSeries timeSeries = session.feature(TimeSeries.class);
            final Random random = new Random();

            for (int i = 0; i < 25; i++) {
                timeSeries.append(topicPath, Double.class, random.nextDouble())
                    .join();
            }

            timeSeries.edit(topicPath, 10, Double.class, 3.14)
                .join();

            final TimeSeries.QueryResult<Bytes> queryResult =
                timeSeries.rangeQuery()
                    .from(5)
                    .to(15)
                    .selectFrom(topicPath)
                    .join();

            queryResult.stream()
                .forEach(event -> LOG.info("{} ({}): {}", event.sequence(), event.timestamp(), event.value()));
        }
    }
}
