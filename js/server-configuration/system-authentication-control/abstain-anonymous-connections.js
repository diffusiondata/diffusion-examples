/*******************************************************************************
 * Copyright (C) 2024 Diffusion Data Ltd.
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

const diffusion = require('diffusion');

export async function abstainAnonymousConnections() {
    /// tag::system_authentication_control_abstain_anonymous_connections[]
    // Connect to the server.
    const session1 = await diffusion.connect({
        host: 'localhost',
        port: 8080,
        principal: 'admin',
        credentials: 'password'
    });

    const authenticationScript = session1.security.authenticationScriptBuilder()
        .abstainAnonymousConnections()
        .build();
    await session1.security.updateAuthenticationStore(authenticationScript);

    const session2 = await diffusion.connect({
        host: 'localhost',
        port: 8080,
        principal: 'control',
        credentials: 'password'
    });
    const authenticator = {
        authenticate: (
            principal,
            credentials,
            sessionProperties,
            proposedPorperties,
            callback
        ) => {
            if (credentials === null) {
                callback.deny();
            } else {
                callback.allow();
            }
        },
        onClose: () => {},
        onError: () => {}
    };

    await session2.security.setAuthenticator('after-system-handler', authenticator);

    try {
        await connect({
            host: 'localhost',
            port: 8080
        });
        /// tag::log
        fail('Anonymous connection should be denied');
        /// end::log
    }
    catch (err) {
        // expected to fail
        console.error('Could not connect', err.message);
    }

    /// tag::log
    const restoreScript = session1.security.authenticationScriptBuilder()
        .denyAnonymousConnections()
        .allowAnonymousConnections(['CLIENT'])
        .build();
    await session1.security.updateAuthenticationStore(restoreScript);
    /// end::log
    await session1.closeSession();
    await session2.closeSession();
    /// end::system_authentication_control_abstain_anonymous_connections[]
}
