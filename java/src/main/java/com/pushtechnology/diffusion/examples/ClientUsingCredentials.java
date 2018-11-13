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

import java.util.concurrent.ExecutionException;

import com.pushtechnology.diffusion.client.Diffusion;
import com.pushtechnology.diffusion.client.features.Security;
import com.pushtechnology.diffusion.client.session.Session;

/**
 * This demonstrates a client's use of credentials, specifically the ability to
 * change the principal for an active session.
 * <P>
 * This is not a realistic use case on its own, but is shown separately here for
 * clarity.
 *
 * @author Push Technology Limited
 * @since 5.0
 */
public class ClientUsingCredentials {

    private final Session session;
    private final Security security;

    /**
     * Constructor.
     */
    public ClientUsingCredentials(String serverUrl) {
        session =
            Diffusion.sessions().principal("client").password("password")
                .open(serverUrl);
        security = session.feature(Security.class);
    }

    /**
     * Request a change of principal for the session, blocking until a response
     * is received.
     *
     * @param principal the new principal name
     * @param password the password
     * @return true if the principal was changed
     * @throws InterruptedException if interrupted while waiting
     * @throws ExecutionException if the ping failed. The chained
     *         {@link ExecutionException#getCause() cause} provides more
     *         information, e.g. SessionClosedException.
     */
    public boolean changePrincipal(
        String principal,
        String password) throws InterruptedException, ExecutionException {

        return security.changePrincipal(
            principal,
            Diffusion.credentials().password(password))
            .get();
    }

    /**
     * Close the session.
     */
    public void close() {
        session.close();
    }

}
