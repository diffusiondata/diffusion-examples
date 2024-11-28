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

import { connect } from 'diffusion';

export async function allowAnonymousConnections(): Promise<void> {
    /// tag::system_authentication_control_allow_anonymous_connections[]
    // Connect to the server.
    const session = await connect({
        host: 'localhost',
        port: 8080,
        principal: 'admin',
        credentials: 'password'
    });

    const authenticationScript = session.security.authenticationScriptBuilder()
        .allowAnonymousConnections()
        .build();
    await session.security.updateAuthenticationStore(authenticationScript);

    const anonymous = await connect({
        host: 'localhost',
        port: 8080
    });
    console.log(`Anonymous session has been established: ${anonymous.sessionId}`);

    /// tag::log
    const restoreScript = session.security.authenticationScriptBuilder()
        .denyAnonymousConnections()
        .allowAnonymousConnections(['CLIENT'])
        .build();
    await session.security.updateAuthenticationStore(restoreScript);
    /// end::log
    await anonymous.closeSession();
    await session.closeSession();
    /// end::system_authentication_control_allow_anonymous_connections[]
}
