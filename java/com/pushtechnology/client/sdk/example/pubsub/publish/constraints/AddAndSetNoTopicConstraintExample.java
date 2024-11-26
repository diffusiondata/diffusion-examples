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
package com.pushtechnology.client.sdk.example.pubsub.publish.constraints;

import java.util.concurrent.CompletionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pushtechnology.diffusion.client.Diffusion;
import com.pushtechnology.diffusion.client.features.TopicUpdate;
import com.pushtechnology.diffusion.client.features.UpdateConstraint;
import com.pushtechnology.diffusion.client.session.Session;
import com.pushtechnology.diffusion.client.topics.details.TopicSpecification;
import com.pushtechnology.diffusion.client.topics.details.TopicType;
import com.pushtechnology.diffusion.datatype.json.JSON;

public class AddAndSetNoTopicConstraintExample {

    private static final Logger LOG =
        LoggerFactory.getLogger(AddAndSetNoTopicConstraintExample.class);

    public static void main(String[] args) {

        Session session = Diffusion.sessions()
            .principal("admin")
            .password("password")
            .open("ws://localhost:8080");

        TopicSpecification specification = Diffusion.newTopicSpecification(TopicType.JSON);
        JSON value = Diffusion.dataTypes().json()
            .fromJsonString("{ \"diffusion\": \"data\" }");

        // create a topic with a value
        session.feature(TopicUpdate.class)
            .addAndSet("my/topic/path", specification, JSON.class, value).join();
        LOG.info("Topic updated");

        // only allow updates if there is no topic at the given path
        UpdateConstraint constraint = Diffusion.updateConstraints().noTopic();

        try {
            // attempt to update the topic with the constraint, this will fail
            session.feature(TopicUpdate.class)
                .addAndSet("my/topic/path", specification, JSON.class, value, constraint).join();
        }
        catch (CompletionException e) {
            LOG.info("Update failed: {}", e.getCause().getMessage());
        }

        session.close();
    }
}
