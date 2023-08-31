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

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.pushtechnology.diffusion.client.Diffusion;
import com.pushtechnology.diffusion.client.session.Session;
import com.pushtechnology.diffusion.client.session.Session.Listener;
import com.pushtechnology.diffusion.client.session.Session.State;
import com.pushtechnology.diffusion.client.session.reconnect.ReconnectionStrategy;

/**
 * This example class demonstrates the ability to set a custom
 * {@link ReconnectionStrategy} when creating sessions.
 *
 * @author DiffusionData Limited
 * @since 5.5
 */
public class ClientWithReconnectionStrategy {

    private volatile int retries = 0;

    /**
     * Constructor.
     */
    public ClientWithReconnectionStrategy() {

        // Set the maximum amount of time we'll try and reconnect for to 10
        // minutes.
        final int maximumTimeoutDuration = 1000 * 60 * 10;

        // Set the maximum interval between reconnect attempts to 60 seconds.
        final long maximumAttemptInterval = 1000 * 60;

        // Create a new reconnection strategy that applies an exponential
        // backoff
        final ReconnectionStrategy reconnectionStrategy =
            new ReconnectionStrategy() {

                private final ScheduledExecutorService scheduler =
                    Executors.newScheduledThreadPool(1);

                @Override
                public void performReconnection(
                    final ReconnectionAttempt reconnection) {

                    final long exponentialWaitTime =
                        Math.min((long) Math.pow(2, retries++) * 100L,
                            maximumAttemptInterval);

                    scheduler.schedule(
                        new Runnable() {
                            @Override
                            public void run() {
                                reconnection.start();
                            }
                        },
                        exponentialWaitTime, TimeUnit.MILLISECONDS);
                }
            };

        final Session session =
            Diffusion.sessions().reconnectionTimeout(maximumTimeoutDuration)
                .reconnectionStrategy(reconnectionStrategy)
                .open("ws://diffusion.example.com:80");

        session.addListener(new Listener() {
            @Override
            public void onSessionStateChanged(Session session, State oldState,
                State newState) {

                if (newState == State.RECOVERING_RECONNECT) {
                    // The session has been disconnected, and has entered
                    // recovery state. It is during this state that
                    // the reconnect strategy will be called
                }

                if (newState == State.CONNECTED_ACTIVE) {
                    // The session has connected for the first time, or it has
                    // been reconnected.
                    retries = 0;
                }

                if (oldState == State.RECOVERING_RECONNECT) {
                    // The session has left recovery state. It may either be
                    // attempting to reconnect, or the attempt has been aborted;
                    // this will be reflected in the newState.
                }
            }
        });
    }
}
