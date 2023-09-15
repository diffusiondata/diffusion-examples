/*******************************************************************************
 * Copyright (c) 2018, 2022 Push Technology Ltd., All Rights Reserved.
 *
 * Use is subject to license terms.
 *
 * NOTICE: All information contained herein is, and remains the
 * property of Push Technology. The intellectual and technical
 * concepts contained herein are proprietary to Push Technology and
 * may be covered by U.S. and Foreign Patents, patents in process, and
 * are protected by trade secret or copyright law.
 *******************************************************************************/

package com.pushtechnology.diffusion.examples;

import static com.pushtechnology.diffusion.client.Diffusion.newTopicSpecification;
import static com.pushtechnology.diffusion.client.Diffusion.updateConstraints;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pushtechnology.diffusion.client.features.TopicCreationResult;
import com.pushtechnology.diffusion.client.features.TopicUpdate;
import com.pushtechnology.diffusion.client.features.UpdateConstraint;
import com.pushtechnology.diffusion.client.features.UpdateStream;
import com.pushtechnology.diffusion.client.session.Session;
import com.pushtechnology.diffusion.client.topics.details.TopicType;

/**
 * Example showing the use of the {@link TopicUpdate} feature.
 *
 * @author DiffusionData Limited
 * @since 6.2
 */
public final class TopicUpdateExample {
    private static final Logger LOG = LoggerFactory.getLogger(TopicUpdateExample.class);

    private TopicUpdateExample() {
    }

    /**
     * Set an INT_64 topic to 6.
     */
    public static void statelessSet(Session session) {

        session
            .feature(TopicUpdate.class)
            .set("a/path", Long.class, 6L)
            .whenComplete(TopicUpdateExample::updateHandler);
    }

    /**
     * Set an INT_64 topic to 6 if the session holds "a/lock" and its current value is 5.
     */
    public static void statelessSetWithConstraint(Session session) {
        final TopicUpdate update = session.feature(TopicUpdate.class);
        final UpdateConstraint.Factory constraints = updateConstraints();

        session
            // Acquire a lock
            .lock("a/lock")
            // Construct a constraint that requires the lock to be held and the value of the topic to be 5
            .thenApply(lock -> constraints.value(5L).and(constraints.locked(lock)))
            // Increments the topic value from 5 to 6 if the lock is still held
            .thenCompose(constraint -> update.<Long>set("a/path", Long.class, 6L, constraint))
            .whenComplete(TopicUpdateExample::updateHandler);
    }

    /**
     * Set a string topic multiple times using the same updater.
     */
    public static void statelessSetRepeatedly(Session session) {

        final TopicUpdate update = session.feature(TopicUpdate.class);
        final UpdateConstraint.Factory constraints = updateConstraints();

        update
            // Set the value of the topic to a string
            .set("a/path", String.class, "hello")
            // Remove the value of the topic by setting it to null
            .thenCompose(x -> update.<String>set("a/path", String.class, null))
            // Set the value of the topic if it has no value
            .thenCompose(x ->
                update.<String>set("a/path", String.class, "who are you?", constraints.noValue()))
            .whenComplete(TopicUpdateExample::updateHandler);
    }

    /**
     * Set "random/long" with a random long every 5 seconds while the updater is active.
     */
    public static void statelessSetPeriodically(Session session) {
        final TopicUpdate updater = session.feature(TopicUpdate.class);

        final Random random = new Random();
        runPeriodicallyUntilFirstFailure(
            newSingleThreadScheduledExecutor(),
            // Set the topic to a random value
            () -> updater.set("random/long", Long.class, random.nextLong()),
            5,
            SECONDS)
        .whenComplete(TopicUpdateExample::updateHandler);
    }

    /**
     * Add a string topic and set to a new value. If the topic exists it will
     * be updated to the new value.
     */
    public static void addAndSetTopicWithStateless(Session session) {

        final TopicUpdate updater = session.feature(TopicUpdate.class);

        updater
            .addAndSet("a/path", newTopicSpecification(TopicType.STRING), String.class, "hello")
            .thenAccept(result -> {
                if (result == TopicCreationResult.CREATED) {
                    LOG.info("A new topic was created");
                }
                else {
                    LOG.info("An existing topic was updated");
                }
            });
    }

    /**
     * Update an INT_64 topic to 6 using an update stream.
     */
    public static void streamSet(Session session) {

        session
            .feature(TopicUpdate.class)
            // Create an update stream
            .newUpdateStreamBuilder().build("a/path", Long.class)
            // Use the update stream to set the topic value. This will invalidate
            // any update stream that exists for the topic. This stream can be
            // invalidated once this completes.
            .set(6L)
            .whenComplete(TopicUpdateExample::updateHandler);
    }

    /**
     * Set an INT_64 topic to 6 if the session holds "a/lock" and its current value is 5 using an update stream.
     */
    public static void streamSetWithConstraint(Session session) {
        final TopicUpdate update = session.feature(TopicUpdate.class);
        final UpdateConstraint.Factory constraints = updateConstraints();

        session
            .lock("a/lock")
            .thenApply(lock -> constraints.value(5L).and(constraints.locked(lock)))
            // Create an update stream. The constraint will not be evaluated yet.
            .thenApply(constraint -> update.newUpdateStreamBuilder()
                .constraint(constraint).build("a/path", Long.class))
            // Use the update stream to set the topic value. This will evaluate the constraint.
            .thenCompose(updateStream -> updateStream.set(6L))
            .whenComplete(TopicUpdateExample::updateHandler);
    }

    /**
     * Set a string topic multiple times using the same update stream.
     */
    public static void streamSetRepeatedly(Session session) {

        final UpdateConstraint.Factory constraints = updateConstraints();
        final UpdateStream<String> updateStream = session
            .feature(TopicUpdate.class).newUpdateStreamBuilder()
            .constraint(constraints.noValue()).build("a/path", String.class);


        // This sends the path with the value and validates the constraint
        updateStream.set("hello")
            // These send a reference to the update stream with the value. The constraint is not reevaluated.
            .thenCompose(x -> updateStream.set(null))
            .thenCompose(x -> updateStream.set("who are you?"))
            .whenComplete(TopicUpdateExample::updateHandler);
    }

    /**
     * Set a string topic multiple times using the same update stream without waiting for the previous operation to
     * complete.
     */
    public static void streamSetRepeatedlyWithoutWaiting(Session session) {

        final UpdateConstraint.Factory constraints = updateConstraints();
        final UpdateStream<String> updateStream = session
            .feature(TopicUpdate.class).newUpdateStreamBuilder()
            .constraint(constraints.noValue()).build("a/path", String.class);

        // This sends the path with the value and validates the constraint
        updateStream.set("hello");
        // These may be deferred until after the completion of the previous operation
        updateStream.set(null);
        updateStream.set("who are you?");
    }

    /**
     * Set "random/long" with a random long every 5 seconds while the update stream is active.
     */
    public static void streamSetPeriodically(Session session) {
        final UpdateStream<Long> updateStream = session
            .feature(TopicUpdate.class)
            .newUpdateStreamBuilder().build("random/long", Long.class);

        final CompletableFuture<TopicCreationResult> validation = updateStream.validate();

        validation
            .whenComplete((result, ex) -> {
                if (ex != null) {
                    LOG.warn("Failed to validate stream", ex);
                }
            });

        final Random random = new Random();
        validation
            .thenCompose(result -> runPeriodicallyUntilFirstFailure(
                newSingleThreadScheduledExecutor(),
                () -> updateStream.set(random.nextLong()),
                5,
                SECONDS))
            .whenComplete(TopicUpdateExample::updateHandler);
    }

    /**
     * Create a string topic with a stream.
     */
    public static void createTopicWithStream(Session session) {

        final UpdateStream<String> updateStream = session
            .feature(TopicUpdate.class)
            // Creates the update stream. This does not add the topic
            .newUpdateStreamBuilder()
            .specification(newTopicSpecification(TopicType.STRING))
            .build("a/path", String.class);

        updateStream
            // This will add the topic without setting it to any value
            .validate()
            .thenAccept(result -> {
                if (result == TopicCreationResult.CREATED) {
                    LOG.info("The topic was created");
                }
                else {
                    LOG.info("The topic already exist");
                }
            });
    }

    /**
     * Create an update stream that adds a string topic and sets it to a new
     * value. If the topic exists it will be updated to the new value.
     */
    public static void addAndSetTopicWithStream(Session session) {

        final UpdateStream<String> updateStream = session
            .feature(TopicUpdate.class)
            .newUpdateStreamBuilder()
            .specification(newTopicSpecification(TopicType.STRING))
            .build("a/path", String.class);

        updateStream
            // This will add the topic if it is missing and set the value
            .set("hello")
            .thenAccept(result -> {
                if (result == TopicCreationResult.CREATED) {
                    LOG.info("A new topic was created");
                }
                else {
                    LOG.info("An existing topic was updated");
                }
            });
    }

    /**
     * Schedule a periodic set operation as long as the update source is active.
     *
     * @return a future representing the task
     */
    private static CompletableFuture<?> runPeriodicallyUntilFirstFailure(
            ScheduledExecutorService executor,
            Supplier<CompletableFuture<?>> task,
            long period,
            TimeUnit unit) {

        final CompletableFuture<?> taskHandle = new CompletableFuture<>();
        scheduleNextUpdate(executor, task, period, unit, taskHandle);
        return taskHandle;
    }

    private static void scheduleNextUpdate(
        ScheduledExecutorService executor,
        Supplier<CompletableFuture<?>> task,
        long period,
        TimeUnit unit,
        CompletableFuture<?> taskHandle) {

        executor
            .schedule(
                () -> {
                    // Skip if task completed or cancelled
                    if (taskHandle.isDone()) {
                        return;
                    }

                    // Send update
                    task
                        .get()
                        .whenComplete((result, ex) -> {
                            if (ex != null) {
                                // Complete task exceptionally if update failed
                                taskHandle.completeExceptionally(ex);
                            }
                            else {
                                // If task completed or cancelled, schedule next execution
                                if (!taskHandle.isDone()) {
                                    scheduleNextUpdate(executor, task, period, unit, taskHandle);
                                }
                            }
                        });
                },
                period,
                unit);
    }

    private static <T> void updateHandler(@SuppressWarnings("unused")T result, Throwable ex) {
        if (ex != null) {
            LOG.error("Update failed", ex);
        }
    }
}
