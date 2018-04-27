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
package com.pushtechnology.diffusion.examples;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pushtechnology.diffusion.client.Diffusion;
import com.pushtechnology.diffusion.client.features.Topics;
import com.pushtechnology.diffusion.client.features.Topics.UnsubscribeReason;
import com.pushtechnology.diffusion.client.features.Topics.ValueStream;
import com.pushtechnology.diffusion.client.session.Session;
import com.pushtechnology.diffusion.client.session.Session.SessionLock;
import com.pushtechnology.diffusion.client.session.Session.SessionLockScope;
import com.pushtechnology.diffusion.client.topics.details.TopicSpecification;
import com.pushtechnology.diffusion.client.types.TopicPermission;

/**
 * An example of a client that uses session locks to coordinate actions with
 * other sessions.
 *
 * <p>
 * In this example, a single session receives and processes updates from the
 * topic {@code topicA}. Each client instance running this code creates a
 * session and competes for the session lock {@code lockA}. The session that is
 * assigned the session lock will subscribe to the topic and log updates.
 *
 * <p>
 * {@link SessionLockScope#UNLOCK_ON_CONNECTION_LOSS UNLOCK_ON_CONNECTION_LOSS}
 * session locks are used. If the session that owns the session lock loses its
 * connection to the server, the server will reassign the lock to another
 * session. This example uses a session listener to independently detect the
 * connection loss, unsubscribe, unregister the stream listening for updates,
 * and compete for the lock again.
 *
 * <p>
 * The locking protocol has races documented under {@link SessionLock}. In the
 * context of this example, the consequences are:
 * <ul>
 * <li>There may be a transient period where two sessions are subscribed to the
 * topic, and both process the same update.
 * <li>A session acquiring a lock may miss one or more updates that were not
 * processed by the session that previously held the lock.
 * </ul>
 *
 * <h2>Security note</h2>
 *
 * <p>
 * To run this example, the "client" principal must be granted
 * {@link TopicPermission#ACQUIRE_LOCK ACQUIRE_LOCK} permission to
 * {@code lockA}.
 *
 * @author Push Technology Limited
 * @since 6.1
 */
public class ClientUsingSessionLocks {

    private static final Logger LOG =
        LoggerFactory.getLogger(ClientUsingSessionLocks.class);

    private static final String LOCK_NAME = "lockA";
    private static final String TOPIC_PATH = "topicA";

    private final Session session;

    private final ValueStream<String> stream = new LogUpdates();

    /**
     * Construct a request handling application.
     *
     * @param serverURL url of the server to connect to
     */
    public ClientUsingSessionLocks(String serverURL) {

        // The "client" principal must have ACQUIRE_LOCK permission, see note in
        // class Javadoc.
        session = Diffusion.sessions().principal("client").password("password")
            .open(serverURL);
    }

    /**
     * Start competing for the lock.
     */
    public void start() {
        session.addListener((s, oldState, newState) -> {
            if (newState.isClosed()) {
                onLockLost();
            }
        });

        requestLock();
    }

    private void requestLock() {
        session.lock(LOCK_NAME, SessionLockScope.UNLOCK_ON_CONNECTION_LOSS)
            .thenAccept(lock -> onLockAcquired());
    }

    private void onLockAcquired() {
        final Topics topics = session.feature(Topics.class);
        topics.subscribe(TOPIC_PATH);
        topics.addStream(TOPIC_PATH,  String.class, stream);
    }

    private void onLockLost() {
        final Topics topics = session.feature(Topics.class);

        // Remove the stream from the local registry. This will prevent
        // processing of updates that may already be queued for this session and
        // will be delivered on reconnection.
        topics.removeStream(stream);

        // Unsubscribe from the topic. This will not take effect until this
        // session has reconnected to the server.
        topics.unsubscribe(TOPIC_PATH);

        // Compete for the lock again. This will not take effect until this
        // session has reconnected to the server, and will be processed after
        // the unsubscription.
        requestLock();
    }

    /**
     * Close the session. If the session owned the lock, the server is free to
     * reassign it to another session.
     */
    public void close() {
        session.close();
    }

    /**
     * Log updates received for a topic.
     */
    private static class LogUpdates extends Topics.ValueStream.Default<String> {

        @Override
        public void onSubscription(
            String topicPath,
            TopicSpecification specification) {

            LOG.info("onSubscription({})", topicPath);
        }

        @Override
        public void onUnsubscription(String topicPath,
            TopicSpecification specification, UnsubscribeReason reason) {

            LOG.info("onUnsubscription({})", topicPath);
        }

        @Override
        public void onValue(
            String topicPath,
            TopicSpecification specification,
            String oldValue,
            String newValue) {

            LOG.info("onValue({}, {})", topicPath, newValue);
        }
    }
}
