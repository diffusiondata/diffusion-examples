/*******************************************************************************
 * Copyright (C) 2023 - 2024 DiffusionData Ltd.
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
package com.pushtechnology.client.sdk.example.connection.resilience;

import com.pushtechnology.diffusion.client.Diffusion;
import com.pushtechnology.diffusion.client.session.Session;
import com.pushtechnology.diffusion.client.session.reconnect.ReconnectionStrategy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ReconnectionStrategyExample {

    private static final Logger LOG =
        LoggerFactory.getLogger(ReconnectionStrategyExample.class);

    public static void main(String[] args) throws Exception{

        final Session session = Diffusion.sessions()
            .principal("admin")
            .password("password")
            .reconnectionStrategy(new MyReconnectionStrategy())
            .open("ws://localhost:8080");

        LOG.info("Connected, session identifier: '{}'.", session.getSessionId());

        // Insert work here

        session.close();
    }

    static class MyReconnectionStrategy implements ReconnectionStrategy {

        private int retries = 0;
        private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

        @Override
        public void performReconnection(ReconnectionAttempt reconnectionAttempt) {
            if (retries < 10) {
                retries++;
                scheduler.schedule(reconnectionAttempt::start, 3000, TimeUnit.MILLISECONDS);
            }
            else {
                reconnectionAttempt.abort();
            }
        }
    }
}
