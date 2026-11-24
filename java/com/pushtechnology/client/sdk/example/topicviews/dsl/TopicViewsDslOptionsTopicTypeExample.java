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
package com.pushtechnology.client.sdk.example.topicviews.dsl;

import static java.util.concurrent.TimeUnit.SECONDS;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pushtechnology.diffusion.client.Diffusion;
import com.pushtechnology.diffusion.client.features.TimeSeries;
import com.pushtechnology.diffusion.client.features.Topics;
import com.pushtechnology.diffusion.client.features.control.topics.views.TopicView;
import com.pushtechnology.diffusion.client.session.Session;
import com.pushtechnology.diffusion.client.topics.details.TopicType;

/**
 * This example demonstrates how to specify the topic type in a topic view.
 * <P>
 * A topic view is created that maps a source topic to a new topic with a
 * different topic type, converting an INT64 topic into a TIME_SERIES topic.
 *
 * @author DiffusionData Limited
 */
public class TopicViewsDslOptionsTopicTypeExample {

    private static final Logger LOG =
        LoggerFactory.getLogger(TopicViewsDslOptionsTopicTypeExample.class);

    public static void main(String[] args) throws Exception {

        final Session session = Diffusion.sessions()
            .principal("admin")
            .password("password")
            .open("ws://localhost:8080");

        final Topics topics = session.feature(Topics.class);
        final String topicPath = "my/topic/path";

        topics.addAndSet(
                topicPath,
                Diffusion.newTopicSpecification(TopicType.INT64), Long.class, 0L)
            .join();

        final TopicView view = topics.createTopicView("topic_view_1",
                "map my/topic/path to views/archive/<path(0)> type TIME_SERIES")
            .join();

        LOG.info("Topic View {} has been created", view.getName());

        for (int i = 0; i < 4; i++) {
            topics.set(topicPath, Long.class, System.currentTimeMillis()).join();
            SECONDS.sleep(1);
        }

        final TimeSeries.QueryResult<Long> queryResult =
            session.feature(TimeSeries.class)
                .rangeQuery()
                .forValues()
                .fromStart()
                .as(Long.class)
                .selectFrom("views/archive/my/topic/path").join();

        queryResult.stream().forEach(
            event -> LOG.info("event {} : value {}", event.sequence(), event.value())
        );

        session.close();
    }
}
