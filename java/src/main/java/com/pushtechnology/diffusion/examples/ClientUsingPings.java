/*******************************************************************************
 * Copyright (C) 2014, 2015 Push Technology Ltd.
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

import com.pushtechnology.diffusion.client.Diffusion;
import com.pushtechnology.diffusion.client.features.Pings;
import com.pushtechnology.diffusion.client.features.Pings.PingContextCallback;
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
    public ClientUsingPings() {
        session =
            Diffusion.sessions().principal("client").password("password")
                .open("ws://diffusion.example.com:80");
        pings = session.feature(Pings.class);
    }

    /**
     * Ping the server.
     *
     * @param context string to log with round trip time
     * @param callback used to return ping reply
     */
    public void ping(String context, PingContextCallback<String> callback) {
        pings.pingServer(context, callback);
    }

    /**
     * Close the session.
     */
    public void close() {
        session.close();
    }

}
