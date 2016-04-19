/*******************************************************************************
 * Copyright (C) 2016 Push Technology Ltd.
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

import static com.pushtechnology.diffusion.client.features.control.topics.TopicUpdateControl.Updater.UpdateCallback;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.pushtechnology.diffusion.client.Diffusion;
import com.pushtechnology.diffusion.client.callbacks.TopicTreeHandler;
import com.pushtechnology.diffusion.client.features.control.topics.TopicControl;
import com.pushtechnology.diffusion.client.features.control.topics.TopicUpdateControl;
import com.pushtechnology.diffusion.client.session.Session;
import com.pushtechnology.diffusion.datatype.json.JSON;

/**
 * A client that creates and updates JSON topics.
 *
 * @author Push Technology Limited
 * @since 5.7
 */
public final class ProducingJson extends AbstractClient {
    private static final Logger LOG = LoggerFactory
        .getLogger(ProducingJson.class);
    private static final UpdateCallback.Default UPDATE_CALLBACK =
        new UpdateCallback.Default();
    private static final TopicControl.AddCallback.Default ADD_CALLBACK =
        new TopicControl.AddCallback.Default();

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

        // Add the JSON topic with an initial value
        topicControl
            .addTopicFromValue(
                "json/random",
                // This value cannot be transformed into a map, will invoke
                // error handling if the client tries to process it
                Diffusion.dataTypes().json().fromJsonString("\"hello\""),
                ADD_CALLBACK);

        // Remove topics when the session closes
        topicControl.removeTopicsWithSession(
            "json",
            new TopicTreeHandler.Default());
    }

    @Override
    public void onConnected(Session session) {
        final TopicUpdateControl.ValueUpdater<JSON> updater = session
            .feature(TopicUpdateControl.class)
            .updater()
            .valueUpdater(JSON.class);

        updateTask = executor.scheduleAtFixedRate(
            new Runnable() {
                @Override
                public void run() {
                    try {
                        // Update the topic with random data
                        updater.update(
                            "json/random",
                            // Converts a RandomData object into a Json map
                            RandomData.toJSON(RandomData.next()),
                            UPDATE_CALLBACK);
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
