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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import com.pushtechnology.diffusion.client.Diffusion;
import com.pushtechnology.diffusion.client.features.Pings;
import com.pushtechnology.diffusion.client.features.Pings.PingDetails;
import com.pushtechnology.diffusion.client.session.Session;

/**
 * This is a simple client example that pings the server and prints out the
 * round-trip time.
 * <P>
 * This uses the 'Pings' feature only.
 *
 * @author Push Technology Limited
 * @since 5.0
 */
public final class ClientUsingPings {

    private final Session session;
    private final Pings pings;

    /**
     * Constructor.
     */
    public ClientUsingPings(String serverUrl) {
        session =
            Diffusion.sessions().principal("client").password("password")
                .open(serverUrl);
        pings = session.feature(Pings.class);
    }

    /**
     * Ping the server, blocking until a response is received.
     *
     * @return the round-trip time in milliseconds
     * @throws InterruptedException if interrupted while waiting
     * @throws ExecutionException if the ping failed. The chained
     *         {@link ExecutionException#getCause() cause} provides more
     *         information, e.g. SessionClosedException.
     */
    public long ping() throws InterruptedException, ExecutionException {
        return pings.pingServer().get().getRoundTripTime();
    }

    /**
     * Ping the server, returning the result asynchronously using a
     * CompletableFuture.
     *
     * @return provides the round-trip time in milliseconds when the ping
     *         response is received
     */
    public CompletableFuture<Long> pingAsync() {
        return pings.pingServer().thenApply(PingDetails::getRoundTripTime);
    }

    /**
     * Close the session.
     */
    public void close() {
        session.close();
    }
}
