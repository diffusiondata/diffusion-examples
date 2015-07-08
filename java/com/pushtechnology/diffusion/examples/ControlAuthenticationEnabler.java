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

import com.pushtechnology.diffusion.client.details.SessionDetails;
import com.pushtechnology.diffusion.client.security.authentication.AuthenticationHandler;
import com.pushtechnology.diffusion.client.types.Credentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a local authentication handler that allows control clients to install
 * their own authentication handlers.
 *
 * @author Push Technology Limited
 * @since 5.0
 */
public class ControlAuthenticationEnabler implements AuthenticationHandler {

    private static final Logger LOG =
        LoggerFactory.getLogger(ControlAuthenticationEnabler.class);

    private static final String AUTH_USER = "auth";
    private static final String AUTH_PASSWORD = "auth_secret";

    @Override
    public void authenticate(
        String principal,
        Credentials credentials,
        SessionDetails sessionDetails,
        Callback callback) {

        LOG.debug("?????????? authentication request for " + principal);
        LOG.debug("?????????? credentials: " + credentials);
        LOG.debug("?????????? sessionDetails: " + sessionDetails);

        if (credentials.getType() == Credentials.Type.PLAIN_PASSWORD) {
            if (AUTH_USER.equals(principal)) {
                if (AUTH_PASSWORD.equals(new String(credentials.toBytes()))) {
                    LOG.debug("?????????? Allowing authenticator");
                    callback.allow();
                    return;
                }
                else {
                    LOG.debug("?????????? Denying authenticator");
                    callback.deny();
                    return;
                }
            }
        }

        LOG.debug("?????????? Abstaining from authentication request");
        callback.abstain();
    }
}