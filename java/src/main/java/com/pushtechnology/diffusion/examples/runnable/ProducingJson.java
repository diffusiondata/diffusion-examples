/*******************************************************************************
 * Copyright (C) 2016, 2023 DiffusionData Ltd.
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

package com.pushtechnology.diffusion.examples.runnable;

import static com.pushtechnology.diffusion.client.Diffusion.newTopicSpecification;
import static com.pushtechnology.diffusion.client.topics.details.TopicSpecification.REMOVAL;
import static com.pushtechnology.diffusion.client.topics.details.TopicType.JSON;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.pushtechnology.diffusion.client.features.TopicUpdate;
import com.pushtechnology.diffusion.client.features.control.topics.TopicControl;
import com.pushtechnology.diffusion.client.session.Session;
import com.pushtechnology.diffusion.client.topics.details.TopicSpecification;
import com.pushtechnology.diffusion.datatype.json.JSON;

/**
 * A client that creates and updates JSON topics.
 *
 * @author DiffusionData Limited
 * @since 5.7
 */
public final class ProducingJson extends AbstractClient {
    private static final Logger LOG = LoggerFactory
        .getLogger(ProducingJson.class);

    private final ScheduledExecutorService executor = Executors
        .newSingleThreadScheduledExecutor();
    private volatile Future<?> updateTask;

    /**
     * Constructor.
     * @param url The URL to connect to
     * @param principal The principal to connect as
     */
    public ProducingJson(String url, String principal) {
        super(url, principal);
    }

    @Override
    public void onStarted(Session session) {
        final TopicControl topicControl = session.feature(TopicControl.class);

        final TopicSpecification specification =
            newTopicSpecification(JSON)
                .withProperty(REMOVAL, "when this session closes");

        topicControl.addTopic("json/random", specification);
    }

    @Override
    public void onConnected(Session session) {
        final TopicUpdate topicUpdate = session.feature(TopicUpdate.class);

        updateTask = executor.scheduleAtFixedRate(
            new Runnable() {
                @Override
                public void run() {
                    try {
                        // Update the topic with random data
                        topicUpdate.set(
                            "json/random",
                            JSON.class,
                            // Converts a RandomData object into a Json map
                            RandomData.toJSON(RandomData.next()));
                    }
                    catch (JsonProcessingException e) {
                        LOG.warn("Failed to transform data", e);
                    }
                }
            },
            0L,
            1L,
            SECONDS);
    }

    @Override
    public void onDisconnected() {
        // Cancel updates when disconnected
        updateTask.cancel(false);
    }

    /**
     * Entry point for the example.
     * @param args The command line arguments
     * @throws InterruptedException If the main thread was interrupted
     */
    public static void main(String[] args) throws InterruptedException {
        final ProducingJson client =
            new ProducingJson("ws://diffusion.example.com:80", "auth");
        client.start("auth_secret");
        client.waitForStopped();
    }
}
