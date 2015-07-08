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
import com.pushtechnology.diffusion.client.features.RegisteredHandler;
import com.pushtechnology.diffusion.client.features.control.clients.AuthenticationControl.ControlAuthenticationHandler;
import com.pushtechnology.diffusion.client.types.Credentials;
import java.io.PrintStream;

/**
 *
 * @author Push Technology Limited
 * @since 5.0
 */
public class ExampleControlAuthenticationHandler
    implements ControlAuthenticationHandler {

    private static final PrintStream OUT = System.out;

    @Override
    public void authenticate(
        String principal,
        Credentials credentials,
        SessionDetails sessionDetails,
        Callback callback) {

        OUT.println("ExampleControlAuthenticationHandler" +
            " asked to authenticate");
        OUT.println("Principal: " + principal);
        OUT.println("Credentials: " + credentials);
        OUT.println("-> " + new String(credentials.toBytes()));
        OUT.println("SessionDetails: " + sessionDetails);

        callback.abstain();
    }

    @Override
    public void onActive(final RegisteredHandler registeredHandler) {
    }

    @Override
    public void onClose() {
    }
}
