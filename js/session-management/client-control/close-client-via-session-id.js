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
/// tag::log
const { promiseWithResolvers } = require('../../../../test/util');
/// end::log

export async function clientControlCloseClientViaSessionId() {
    /// tag::log
    const { promise, resolve } = promiseWithResolvers();
    /// end::log
    /// tag::client_control_close_client_via_session_id[]
    // Connect to the server.
    const session1 = await diffusion.connect({
        host: 'localhost',
        port: 8080,
        principal: 'admin',
        credentials: 'password'
    });

    // Connect to the server.
    const session2 = await diffusion.connect({
        host: 'localhost',
        port: 8080,
        principal: 'client',
        credentials: 'password'
    });

    session2.on('close', () => {
        console.log('Session closed');
        /// tag::log
        resolve();
        /// end::log
    });

    await session1.clients.close(session2.sessionId);

    await session1.closeSession();
    /// tag::log
    await promise;
    expect(session2.isClosed()).toBe(true);
    /// end::log
    /// end::client_control_close_client_via_session_id[]
}
