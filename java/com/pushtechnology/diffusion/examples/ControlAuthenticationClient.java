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
import com.pushtechnology.diffusion.client.details.SessionDetails.DetailType;
import com.pushtechnology.diffusion.client.features.control.clients.AuthenticationControl;
import com.pushtechnology.diffusion.client.session.Session;
import java.util.EnumSet;

/**
 * This is a control client which registers an authentication handler with a
 * Diffusion server.
 *
 * @author Push Technology Limited
 * @since 5.0
 */
public final class ControlAuthenticationClient {

    private ControlAuthenticationClient() {
    }

    /**
     * Main entry point for the control client.
     *
     * @param args Commandline arguments, currently ignored.
     * @throws Exception Any exception causes abnormal program termination.
     */
    // CHECKSTYLE.OFF: UncommentedMain
    public static void main(final String[] args) throws Exception {

        final Session session =
            Diffusion.sessions()
                .principal("auth")
                .password("auth_secret")
                .open("ws://diffusion.example.com:80");

        session.feature(AuthenticationControl.class).setAuthenticationHandler(
            "control-client-auth-handler-example",
            EnumSet.allOf(DetailType.class),
            new ExampleControlAuthenticationHandler());

        while (true) {
            Thread.sleep(60000);
        }
    }
    // CHECKSTYLE.ON: UncommentedMain

}
