/*******************************************************************************
 * Copyright (C) 2018 Push Technology Ltd.
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
import static com.pushtechnology.diffusion.client.topics.details.TopicType.INT64;

import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pushtechnology.diffusion.client.Diffusion;
import com.pushtechnology.diffusion.client.features.ClusterRepartitionException;
import com.pushtechnology.diffusion.client.features.IncompatibleTopicException;
import com.pushtechnology.diffusion.client.features.IncompatibleTopicStateException;
import com.pushtechnology.diffusion.client.features.NoSuchTopicException;
import com.pushtechnology.diffusion.client.features.TopicCreationResult;
import com.pushtechnology.diffusion.client.features.Topics;
import com.pushtechnology.diffusion.client.features.Topics.ValueStream.Default;
import com.pushtechnology.diffusion.client.features.UnsatisfiedConstraintException;
import com.pushtechnology.diffusion.client.features.UpdateConstraint;
import com.pushtechnology.diffusion.client.features.control.topics.TopicControl.TopicLicenseLimitException;
import com.pushtechnology.diffusion.client.session.Session;
import com.pushtechnology.diffusion.client.session.SessionClosedException;
import com.pushtechnology.diffusion.client.session.SessionSecurityException;
import com.pushtechnology.diffusion.client.topics.details.TopicSpecification;

/**
 * A client that increments a topic using stateless operations.
 * <p>
 * The client will increment the topic every 5 seconds.
 * <p>
 * Multiple instances of the client can compete to increment the topic without
 * generating an incorrect sequence of updates or knowing about other sessions.
 * <p>
 * The client subscribes to the topic to obtain the latest known value. Every
 * 5 seconds it attempts to increment the topic using the known value as a
 * constraint. If another session has changed the topic value, the update will
 * fail and the client will retry with the latest known value.
 *
 * @author Push Technology Limited
 * @since 6.2
 */
public final class CompetitiveIncrement extends AbstractClient {
    private static final Logger LOG = LoggerFactory
        .getLogger(CompetitiveIncrement.class);
    private static final UpdateConstraint.Factory CONSTRAINTS =
        Diffusion.updateConstraints();

    private final ScheduledExecutorService executor = Executors
        .newSingleThreadScheduledExecutor();

    private volatile Future<?> updateTask;
    private volatile Long value;

    /**
     * Constructor.
     *
     * @param url       The URL to connect to
     * @param principal The principal to connect as
     */
    public CompetitiveIncrement(String url, String principal) {
        super(url, principal);
    }

    @Override
    public void onStarted(Session session) {
        subscribeToPath(session);
    }

    private void subscribeToPath(Session session) {
        // Subscribe to the topic to get the latest value
        final Topics topics = session.feature(Topics.class);
        topics
            .addStream("long/increment", Long.class, new Default<Long>() {
                @Override
                public void onValue(
                    String topicPath,
                    TopicSpecification specification,
                    Long oldValue,
                    Long newValue) {

                    // Update the latest known value
                    value = newValue;
                }
            });
        topics
            .subscribe("long/increment")
            .whenComplete((result, ex) -> {
                if (ex != null) {
                    LOG.warn("Subscription failed", ex);
                    stop();
                }
                else {
                    initialiseTopic(topics);
                }
            });
    }

    private void initialiseTopic(Topics topics) {
        // Create the topic and initialise it to 0 if it does not exist
        topics
            .addAndSet(
                "long/increment",
                newTopicSpecification(INT64),
                Long.class,
                0L,
                CONSTRAINTS.noTopic())
            .whenComplete((result, ex) -> {
                if (ex != null) {
                    final Throwable cause = ex.getCause();
                    if (cause instanceof UnsatisfiedConstraintException) {
                        // The constraint was unsatisfied so the topic must
                        // already exist. Begin incrementing the topic value.
                        LOG.info("Topic exists");
                        scheduleIncrement(topics);
                    }
                    else if (cause instanceof IncompatibleTopicException) {
                        // A topic exists with a different type. This implies
                        // that something else is responsible for the path.
                        LOG.warn("An existing topic is not compatible");
                        stop();
                    }
                    else if (cause instanceof IncompatibleTopicStateException) {
                        // A topic exists, managed by a component that does not
                        // allow updates from the API like fanout. Something
                        // else is responsible for the path.
                        LOG.warn("An existing topic is managed by a different component");
                        stop();
                    }
                    else if (cause instanceof TopicLicenseLimitException) {
                        // The topic can't be created because the server has
                        // reached the limit it is licensed for.
                        LOG.warn("License limit reached", ex);
                        stop();
                    }
                    else {
                        LOG.warn("Topic creation failed", ex);
                        stop();
                    }
                }
                else if (result == TopicCreationResult.CREATED) {
                    // Begin incrementing the topic value.
                    LOG.info("Topic created");
                    scheduleIncrement(topics);
                }
            });
    }

    /**
     * Schedule an increment in 5 seconds.
     */
    private void scheduleIncrement(Topics topics) {
        updateTask = executor.schedule(
            () -> performIncrement(topics),
            5L,
            TimeUnit.SECONDS);
    }

    /**
     * Perform an increment of the latest known value.
     */
    private void performIncrement(Topics topics) {
        final Long currentValue = value;

        topics
            .set(
                "long/increment",
                Long.class,
                currentValue + 1,
                CONSTRAINTS.value(currentValue))
            .whenComplete((result, ex) -> {
                if (ex != null) {
                    // Handle any failure
                    handleIncrementFailure(topics, ex);
                }
                else {
                    // On success schedule the next update
                    LOG.info("Topic incremented {} -> {}", currentValue, currentValue + 1);
                    scheduleIncrement(topics);
                }
            });
    }

    private void handleIncrementFailure(Topics topics, Throwable ex) {
        final Throwable cause = ex.getCause();
        if (cause instanceof ClusterRepartitionException) {
            // The cluster was repartitioning during the increment.
            // Retry incrementing the topic until successful.
            performIncrement(topics);
        }
        else if (cause instanceof UnsatisfiedConstraintException) {
            // The constraint was not satisfied, another session must have
            // updated the topic. Retry incrementing the topic with the latest
            // known value until successful.
            performIncrement(topics);
        }
        else if (cause instanceof NoSuchTopicException) {
            // The topic has been removed since it was created. This could be
            // recoverable, but it implies that something else has taken
            // responsibility for the path. Attempting to recover could cause
            // this and something else to compete over the topic.
            LOG.warn("The topic has been deleted");
            stop();
        }
        else if (cause instanceof IncompatibleTopicException) {
            // The topic has been replaced with a topic of a different type
            // since it was created. This implies that something else has taken
            // responsibility for the path.
            LOG.warn("The topic is not compatible");
            stop();
        }
        else if (cause instanceof IncompatibleTopicStateException) {
            // The topic is managed by a component that does not allow updates
            // from the API like fanout. Something else has taken
            // responsibility for the path.
            LOG.warn("The topic is managed by a different component");
            stop();
        }
        else if (cause instanceof SessionSecurityException) {
            // The session does't have permission update update the path.
            // This is not recoverable.
            LOG.warn("The session does't have permission to update the path");
            stop();
        }
        else if (cause instanceof SessionClosedException) {
            // The session has closed. This is not recoverable.
            LOG.warn("The session has closed");
            stop();
        }
    }

    @Override
    public void onDisconnected() {
        final Future<?> task = this.updateTask;
        if (task != null) {
            task.cancel(false);
        }
    }

    /**
     * Entry point for the example.
     * @param args The command line arguments
     * @throws InterruptedException If the main thread was interrupted
     */
    public static void main(String[] args) throws InterruptedException {
        final CompetitiveIncrement client =
            new CompetitiveIncrement("ws://diffusion.example.com:80", "auth");
        client.start("auth_secret");
        client.waitForStopped();
    }
}
