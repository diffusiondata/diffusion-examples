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

import static com.pushtechnology.diffusion.client.session.Session.State.CONNECTING;

import java.util.concurrent.CountDownLatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pushtechnology.diffusion.client.Diffusion;
import com.pushtechnology.diffusion.client.callbacks.ErrorReason;
import com.pushtechnology.diffusion.client.session.Session;
import com.pushtechnology.diffusion.client.session.SessionFactory;

/**
 * Abstract client. Supporting a simplified state model and starting and
 * stopping the client.
 *
 * @author Push Technology Limited
 * @since 5.7
 */
public abstract class AbstractClient {
    private static final Logger LOG = LoggerFactory
        .getLogger(AbstractClient.class);

    private final String url;
    private final String principal;

    private boolean running = false;
    private Session currentSession;
    private CountDownLatch waitForStoppedLatch;

    /**
     * Constructor.
     * @param url The URL to connect to
     * @param principal The principal to connect as
     */
    public AbstractClient(String url, String principal) {
        this.url = url;
        this.principal = principal;
    }

    /**
     * Start the client.
     * @param password The password to connect with
     * @throws IllegalStateException If the client is already running
     */
    public final void start(String password) {
        synchronized (this) {
            if (running) {
                throw new IllegalStateException("Already running");
            }
            running = true;
            waitForStoppedLatch = new CountDownLatch(1);
        }

        Diffusion.sessions()
            .principal(principal)
            .password(password)
            .listener(new Session.Listener() {
                @Override
                public void onSessionStateChanged(
                    Session session,
                    Session.State oldState,
                    Session.State newState) {

                    synchronized (AbstractClient.this) {
                        if (CONNECTING == oldState && newState.isConnected()) {
                            onStarted(session);
                        }

                        if (!oldState.isConnected() && newState.isConnected()) {
                            currentSession = session;
                            onConnected(session);
                        }

                        if (oldState.isConnected() && !newState.isConnected()) {
                            onDisconnected();
                        }

                        if (newState.isClosed()) {
                            waitForStoppedLatch.countDown();
                            waitForStoppedLatch = null;
                            running = false;
                        }
                    }
                }
            })
            .open(url, new SessionFactory.OpenCallback() {
                @Override
                public void onError(ErrorReason errorReason) {
                    synchronized (AbstractClient.this) {
                        AbstractClient.this.onError(errorReason);
                    }
                }

                @Override
                public void onOpened(Session session) {
                }
            });
    }

    /**
     * Stop the client. Returns immediately, does not wait for the client to
     * stop.
     * @throws IllegalStateException If the client is not running
     */
    public final synchronized void stop() {
        if (!running) {
            throw new IllegalStateException("Not currently running");
        }

        currentSession.close();
    }

    /**
     * Wait for the client to stop.
     * @throws InterruptedException If the thread was interrupted while waiting
     *  for the client to stop
     */
    public final void waitForStopped() throws InterruptedException {
        final CountDownLatch currentLatch;
        synchronized (this) {
            if (waitForStoppedLatch == null) {
                return;
            }
            currentLatch = waitForStoppedLatch;
        }
        currentLatch.await();
    }

    /**
     * Notified when the client starts. A new session has been opened.
     * @param session The session that opened
     */
    public void onStarted(Session session) {
        LOG.debug("Client started");
    }

    /**
     * Notified when the client connects. Either initially or after recovering
     * from a disconnection.
     * @param session The session that connected
     */
    public void onConnected(Session session) {
        LOG.debug("Client connected");
    }

    /**
     * Notified when the client disconnects. Either temporarily or permanently.
     */
    public void onDisconnected() {
        LOG.debug("Client disconnected");
    }

    /**
     * Notified when an error prevents the start of the client.
     * @param errorReason The error reason
     */
    public void onError(ErrorReason errorReason) {
        LOG.error("Failed to start client: {}", errorReason);
    }
}
