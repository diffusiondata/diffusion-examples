/*******************************************************************************
 * Copyright (C) 2014, 2017 Push Technology Ltd.
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
package com.pushtechnology.diffusion.examples;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.pushtechnology.diffusion.client.Diffusion;
import com.pushtechnology.diffusion.client.features.control.topics.TopicControl;
import com.pushtechnology.diffusion.client.features.control.topics.TopicUpdateControl;
import com.pushtechnology.diffusion.client.features.control.topics.TopicUpdateControl.UpdateSource;
import com.pushtechnology.diffusion.client.features.control.topics.TopicUpdateControl.Updater;
import com.pushtechnology.diffusion.client.features.control.topics.TopicUpdateControl.Updater.UpdateCallback;
import com.pushtechnology.diffusion.client.features.control.topics.TopicUpdateControl.ValueUpdater;
import com.pushtechnology.diffusion.client.session.Session;
import com.pushtechnology.diffusion.client.topics.details.TopicType;

/**
 * An example of using a control client as an event feed to a topic.
 * <P>
 * This uses the 'TopicControl' feature to create a topic and the
 * 'TopicUpdateControl' feature to send updates to it.
 * <P>
 * To send updates to a topic, the client session requires the 'update_topic'
 * permission for that branch of the topic tree.
 *
 * @author Push Technology Limited
 * @since 5.0
 */
public class ControlClientAsUpdateSource {

    private static final String TOPIC_NAME = "Feeder";

    private final Session session;
    private final UpdateCallback updateCallback;

    /**
     * Constructor.
     *
     * @param callback for updates
     */
    public ControlClientAsUpdateSource(UpdateCallback callback) {

        updateCallback = callback;

        session =
            Diffusion.sessions().principal("control").password("password")
                .open("ws://diffusion.example.com:80");
    }

    /**
     * Start the feed.
     *
     * @param provider the provider of prices
     * @param scheduler a scheduler service to schedule a periodic feeder task
     * @throws TimeoutException if the topic was not created within 5 seconds
     * @throws ExecutionException if topic creation failed
     * @throws InterruptedException if the current thread was interrupted whilst
     *         waiting for the topic to be created
     */
    public void start(
        final PriceProvider provider,
        final ScheduledExecutorService scheduler)
        throws InterruptedException, ExecutionException, TimeoutException {

        // Add the topic and when created notify the server that the topic
        // should be removed when the session closes.
        final TopicControl topicControl = session.feature(TopicControl.class);

        topicControl.addTopic(
            TOPIC_NAME,
            TopicType.STRING).get(5, TimeUnit.SECONDS);

        topicControl.removeTopicsWithSession(TOPIC_NAME);

        // Declare a custom update source implementation. When the source is set
        // as active start a periodic task to poll the provider every second and
        // update the topic. When the source is closed, stop the scheduled task.
        final UpdateSource source = new UpdateSource.Default() {
            private ScheduledFuture<?> theFeeder;

            @Override
            public void onActive(String topicPath, Updater updater) {
                theFeeder =
                    scheduler.scheduleAtFixedRate(
                        new FeederTask(provider,
                            updater.valueUpdater(String.class)),
                        1, 1, TimeUnit.SECONDS);
            }

            @Override
            public void onClose(String topicPath) {
                if (theFeeder != null) {
                    theFeeder.cancel(true);
                }
            }
        };

        final TopicUpdateControl updateControl =
            session.feature(TopicUpdateControl.class);

        updateControl.registerUpdateSource(TOPIC_NAME, source);

    }

    /**
     * Close the session.
     */
    public void close() {
        session.close();
    }

    /**
     * Periodic task to poll from provider and send update to server.
     */
    private final class FeederTask implements Runnable {

        private final PriceProvider priceProvider;
        private final ValueUpdater<String> priceUpdater;

        private FeederTask(PriceProvider provider,
            ValueUpdater<String> updater) {
            priceProvider = provider;
            priceUpdater = updater;
        }

        @Override
        public void run() {
            priceUpdater.update(
                TOPIC_NAME,
                priceProvider.getPrice(),
                updateCallback);
        }

    }

    /**
     * Interface of a price provider that can periodically be polled for a
     * price.
     */
    public interface PriceProvider {
        /**
         * Get the current price.
         *
         * @return current price as a decimal string
         */
        String getPrice();
    }
}
