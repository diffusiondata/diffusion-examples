/*******************************************************************************
 * Copyright (C) 2018, 2022 Push Technology Ltd.
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
import static com.pushtechnology.diffusion.client.Diffusion.updateConstraints;

import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pushtechnology.diffusion.client.features.ClusterRoutingException;
import com.pushtechnology.diffusion.client.features.IncompatibleTopicException;
import com.pushtechnology.diffusion.client.features.InvalidUpdateStreamException;
import com.pushtechnology.diffusion.client.features.NoSuchTopicException;
import com.pushtechnology.diffusion.client.features.TopicUpdate;
import com.pushtechnology.diffusion.client.features.UnsatisfiedConstraintException;
import com.pushtechnology.diffusion.client.features.UpdateConstraint;
import com.pushtechnology.diffusion.client.features.UpdateStream;
import com.pushtechnology.diffusion.client.session.PermissionsException;
import com.pushtechnology.diffusion.client.session.Session;
import com.pushtechnology.diffusion.client.session.SessionClosedException;
import com.pushtechnology.diffusion.client.topics.details.TopicSpecification;
import com.pushtechnology.diffusion.client.topics.details.TopicType;

/**
 * Example showing how session locks and the {@link TopicUpdate} feature can
 * be used to coordinate topic control between sessions.
 * <p>
 * Session locks are used to ensure a single instance of the client is
 * responsible for updating a path at any one time. Responsibility is
 * transferred when a session loses connection. The client handles transient
 * issues caused by cluster repartitioning.
 *
 * @author Push Technology Limited
 * @since 6.2
 */
public final class CoordinatingSessionExample extends AbstractClient {
    private static final Logger LOG = LoggerFactory.getLogger(CoordinatingSessionExample.class);
    private static final long PERIOD = 5000;
    private static final TimeUnit UNIT = TimeUnit.MILLISECONDS;

    private final ScheduledExecutorService executor = Executors
        .newSingleThreadScheduledExecutor();
    private final Random random = new Random();

    /**
     * Constructor.
     *
     * @param url       The URL to connect to
     * @param principal The principal to connect as
     */
    public CoordinatingSessionExample(String url, String principal) {
        super(url, principal);
    }

    @Override
    public void onConnected(Session session) {
        updatePath(session, "topic");
    }

    private void updatePath(Session session, String path) {
        session
            // The topic path being updated is used as the lock name
            .lock(path, Session.SessionLockScope.UNLOCK_ON_CONNECTION_LOSS)
            .thenAccept(lock -> onLockAcquired(session, path, lock));
    }

    private void onLockAcquired(Session session, String path, Session.SessionLock lock) {
        final TopicUpdate topicUpdate = session.feature(TopicUpdate.class);

        // If this session still owns the lock, update a String topic, setting
        // its value to the session ID of this session. The topic will be
        // created if it doesn't already exist.
        final UpdateConstraint locked = updateConstraints().locked(lock);
        topicUpdate.addAndSet(
            path + "/last_updater",
            newTopicSpecification(TopicType.STRING),
            String.class,
            session.getSessionId().toString(),
            locked);

        // Create a factory for update streams
        final TopicSpecification specification = newTopicSpecification(TopicType.INT64);
        final Supplier<UpdateStream<Long>> updateStreamFactory =
            () -> topicUpdate.createUpdateStream(path, specification, Long.class, locked);

        final UpdateStream<Long> updateStream = updateStreamFactory.get();
        updateStream
            .validate()
            .whenComplete((result, ex) -> {
                if (ex != null) {
                    LOG.warn("Unable to initialise first value stream. Unable to begin updating path.", ex);
                    // The lock will be released with the session
                    stop();
                }
                else {
                    // Begin updating with the stream
                    final UpdateTask updateTask = new UpdateTask(updateStreamFactory, updateStream, random::nextLong);
                    updateTask.scheduleUpdate();
                }
            });
    }

    private final class UpdateTask {
        private final Supplier<UpdateStream<Long>> updateStreamFactory;
        private final Supplier<Long> valueSupplier;
        private volatile UpdateStream<Long> updateStream;

        private UpdateTask(
            Supplier<UpdateStream<Long>> updateStreamFactory,
            UpdateStream<Long> updateStream,
            Supplier<Long> valueSupplier) {

            this.updateStreamFactory = updateStreamFactory;
            this.updateStream = updateStream;
            this.valueSupplier = valueSupplier;
        }

        private void scheduleUpdate() {
            executor.schedule((Runnable) this::performUpdate, PERIOD, UNIT);
        }

        private void performUpdate() {
            // Generate the next value and send update
            performUpdate(valueSupplier.get());
        }

        private void performUpdate(long value) {
            // Send update
            updateStream
                .set(value)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        handleUpdateFailure(value, ex);
                    }
                    else {
                        scheduleUpdate();
                    }
                });
        }

        private void handleUpdateFailure(long value, Throwable ex) {
            final Throwable cause = ex.getCause();
            if (cause instanceof ClusterRoutingException) {
                // Replace stream and retry the update
                updateStream = updateStreamFactory.get();
                performUpdate(value);
            }
            else if (cause instanceof UnsatisfiedConstraintException) {
                // Another session gained the session lock and is now
                // responsible for the the topic.
                LOG.warn("Another session has gained the responsibility for updating the topic");
                // The lock will be released with the session
                stop();
            }
            else if (cause instanceof NoSuchTopicException) {
                // The topic has been removed since the stream was created,
                // this could be recoverable but it implies that something
                // else has taken responsibility for the path. Attempting
                // to recover could cause this and something else to compete
                // over the topic.
                LOG.warn("The topic has been deleted");
                // The lock will be released with the session
                stop();
            }
            else if (cause instanceof InvalidUpdateStreamException) {
                // Something other than the stream changed the topic, this
                // could be recoverable but it implies that something else has
                // taken responsibility for the topic. Attempting to recover
                // could cause this and something else to compete over the
                // topic. For example this session could have lost connection
                // and another has taken over its role.
                LOG.warn("The update stream is no longer valid");
                // The lock will be released with the session
                stop();
            }
            else if (cause instanceof IncompatibleTopicException) {
                // Following an attempt to replace the stream, it was found the
                // topic has been removed and replaced with an incompatible
                // topic. This implies that something else has taken
                // responsibility for the path.
                LOG.warn("The topic is not compatible");
                // The lock will be released with the session
                stop();
            }
            else if (cause instanceof PermissionsException) {
                // The session does't have permission update update the path.
                // This is not recoverable.
                LOG.warn("The session doesn't have permission to update the path");
                // The lock will be released with the session
                stop();
            }
            else if (cause instanceof SessionClosedException) {
                // The session has closed. This is not recoverable.
                LOG.warn("The session has closed");
            }
        }
    }
}
