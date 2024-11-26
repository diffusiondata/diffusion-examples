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
package com.pushtechnology.client.sdk.example.topicviews.api;

import com.pushtechnology.diffusion.client.Diffusion;
import com.pushtechnology.diffusion.client.features.Topics;
import com.pushtechnology.diffusion.client.features.control.topics.TopicControl;
import com.pushtechnology.diffusion.client.features.control.topics.views.TopicView;
import com.pushtechnology.diffusion.client.session.Session;
import com.pushtechnology.diffusion.client.topics.details.TopicSpecification;
import com.pushtechnology.diffusion.client.topics.details.TopicType;
import com.pushtechnology.diffusion.datatype.json.JSON;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ListTopicViewsExample {
    private static final Logger
        LOG = LoggerFactory.getLogger(ListTopicViewsExample.class);

    public static void main(String[] args) {
        Session session = Diffusion.sessions()
            .principal("admin")
            .password("password")
            .open("ws://localhost:8080");

        final Topics topics = session.feature(Topics.class);
        final TopicSpecification mySpec =
            Diffusion.newTopicSpecification(TopicType.JSON);
        final JSON jsonValue = Diffusion.dataTypes().json()
            .fromJsonString("{ \"diffusion\": \"data\" }");

        topics.addAndSet("my/topic/path", mySpec, JSON.class, jsonValue)
            .join();

        topics.createTopicView("topic_view_1", "map my/topic/path to views/<path(0)>")
            .join();

        System.out.println("Topic View <topic_view_1> has been created");

        topics.addAndSet("my/topic/path/array", mySpec, JSON.class, jsonValue)
            .join();

        topics.createTopicView("topic_view_2", "map my/topic/path/array to views/<path(0)>")
            .join();

        System.out.println("Topic View <topic_view_2> has been created");

        List<TopicView> topicViews = topics.listTopicViews().join();

        topicViews.forEach(view -> {
            String name = view.getName();
            String spec = view.getSpecification();
            String roles = view.getRoles().toString();

            System.out.printf("Topic View <%s>: <%s> (<%s>)\n", name, spec, roles);
        });

        topics.removeTopicView("topic_view_1").join();
        topics.removeTopicView("topic_view_2").join();
        session.feature(TopicControl.class).removeTopics("?.*//").join();

        session.close();

        LOG.info("Topic Views listed: {}", topicViews.size());
    }
}
