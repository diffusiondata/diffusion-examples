/*******************************************************************************
 * Copyright (C) 2014, 2023 DiffusionData Ltd.
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

import static com.pushtechnology.diffusion.client.Diffusion.newTopicSpecification;
import static com.pushtechnology.diffusion.client.Diffusion.updateConstraints;
import static com.pushtechnology.diffusion.client.topics.details.TopicSpecification.REMOVAL;
import static com.pushtechnology.diffusion.client.topics.details.TopicType.STRING;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeoutException;

import com.pushtechnology.diffusion.client.Diffusion;
import com.pushtechnology.diffusion.client.features.TopicUpdate;
import com.pushtechnology.diffusion.client.features.UpdateConstraint;
import com.pushtechnology.diffusion.client.features.UpdateStream;
import com.pushtechnology.diffusion.client.features.control.topics.TopicControl;
import com.pushtechnology.diffusion.client.session.Session;
import com.pushtechnology.diffusion.client.topics.details.TopicSpecification;

/**
 * An example of using a control client as an event feed to a topic.
 * <P>
 * This uses the 'TopicControl' feature to create a topic and the
 * 'TopicUpdate' feature to send updates to it.
 * <P>
 * To create the topic, the client session requires the 'modify_topic'
 * permission for that branch of the topic tree.
 * <P>
 * To send updates to a topic, the client session requires the 'update_topic'
 * permission for that branch of the topic tree.
 *
 * @author DiffusionData Limited
 * @since 5.0
 */
public class ControlClientAsExclusiveUpdater {

    private static final String TOPIC_NAME = "Feeder";

    private final Session session;

    /**
     * Constructor.
     */
    public ControlClientAsExclusiveUpdater(String serverUrl) {
        session =
            Diffusion.sessions().principal("control").password("password")
                .open(serverUrl);
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

        // Add the topic with a REMOVAL policy indicating that the topic
        // will be removed when the session no longer exists.
        final TopicControl topicControl = session.feature(TopicControl.class);

        final TopicSpecification specification =
            newTopicSpecification(STRING)
                .withProperty(REMOVAL, "when this session closes");

        topicControl.addTopic(
            TOPIC_NAME,
            specification).get(5, SECONDS);

        final TopicUpdate topicUpdate =
            session.feature(TopicUpdate.class);

        session
            .lock(TOPIC_NAME)
            .thenAccept(lock -> onLockAcquired(lock, provider, scheduler, topicUpdate));
    }

    private static void onLockAcquired(
        Session.SessionLock lock,
        PriceProvider provider,
        ScheduledExecutorService scheduler,
        TopicUpdate topicUpdate) {

        // Use the session lock to create an update stream without any other
        // session competing for a stream for the topic. Start a periodic task
        // to poll the provider every second and update the topic. When the
        // update stream fails, stop the scheduled task and release the lock.
        final UpdateConstraint exclusiveAccessConstraint =
            updateConstraints().locked(lock);
        final UpdateStream<String> updateStream = topicUpdate.newUpdateStreamBuilder()
            .constraint(exclusiveAccessConstraint).build(TOPIC_NAME, String.class);
        final CompletableFuture<Void> failureHandler = new CompletableFuture<>();
        final ScheduledFuture<?> theFeeder = scheduler.scheduleAtFixedRate(
            () -> updateStream
                .set(provider.getPrice())
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        failureHandler.completeExceptionally(ex);
                    }
                }),
            1,
            1,
            SECONDS);
        failureHandler.whenComplete((result, ex) -> {
            theFeeder.cancel(false);
            lock.unlock();
        });
    }

    /**
     * Close the session.
     */
    public void close() {
        session.close();
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
