/*******************************************************************************
 * Copyright (C) 2014, 2016 Push Technology Ltd.
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

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.pushtechnology.diffusion.client.Diffusion;
import com.pushtechnology.diffusion.client.callbacks.TopicTreeHandler;
import com.pushtechnology.diffusion.client.features.control.topics.TopicControl;
import com.pushtechnology.diffusion.client.features.control.topics.TopicControl.AddCallback;
import com.pushtechnology.diffusion.client.features.control.topics.TopicUpdateControl;
import com.pushtechnology.diffusion.client.features.control.topics.TopicUpdateControl.UpdateSource;
import com.pushtechnology.diffusion.client.features.control.topics.TopicUpdateControl.Updater;
import com.pushtechnology.diffusion.client.features.control.topics.TopicUpdateControl.Updater.UpdateCallback;
import com.pushtechnology.diffusion.client.session.Session;
import com.pushtechnology.diffusion.client.topics.details.SingleValueTopicDetails;
import com.pushtechnology.diffusion.client.topics.details.TopicDetails;

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
    private final TopicControl topicControl;
    private final TopicUpdateControl updateControl;
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
        topicControl = session.feature(TopicControl.class);
        updateControl = session.feature(TopicUpdateControl.class);
    }

    /**
     * Start the feed.
     *
     * @param provider the provider of prices
     * @param scheduler a scheduler service to schedule a periodic feeder task
     */
    public void start(
        final PriceProvider provider,
        final ScheduledExecutorService scheduler) {

        // Set up topic details
        final SingleValueTopicDetails.Builder builder =
            topicControl.newDetailsBuilder(
                SingleValueTopicDetails.Builder.class);

        final TopicDetails details =
            builder.metadata(Diffusion.metadata().decimal("Price")).build();

        // Declare a custom update source implementation. When the source is set
        // as active start a periodic task to poll the provider every second and
        // update the topic. When the source is closed, stop the scheduled task.
        final UpdateSource source = new UpdateSource.Default() {
            private ScheduledFuture<?> theFeeder;

            @Override
            public void onActive(String topicPath, Updater updater) {
                theFeeder =
                    scheduler.scheduleAtFixedRate(
                        new FeederTask(provider, updater),
                        1, 1, TimeUnit.SECONDS);
            }

            @Override
            public void onClose(String topicPath) {
                if (theFeeder != null) {
                    theFeeder.cancel(true);
                }
            }
        };

        // Create the topic. When the callback indicates that the topic has been
        // created then register the topic source for the topic and request
        // that it is removed when the session closes.
        topicControl.addTopic(
            TOPIC_NAME,
            details,
            new AddCallback.Default() {
                @Override
                public void onTopicAdded(String topic) {
                    topicControl.removeTopicsWithSession(
                        topic,
                        new TopicTreeHandler.Default());
                    updateControl.registerUpdateSource(topic, source);
                }
            });

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
        private final Updater priceUpdater;

        private FeederTask(PriceProvider provider, Updater updater) {
            priceProvider = provider;
            priceUpdater = updater;
        }

        @Override
        public void run() {
            priceUpdater.update(
                TOPIC_NAME,
                Diffusion.content().newContent(priceProvider.getPrice()),
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
