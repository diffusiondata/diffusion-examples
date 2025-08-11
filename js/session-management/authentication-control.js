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
const { PartiallyOrderedCheckpointTester } = require('../../../test/util');
/// end::log

export async function sessionManagementAuthenticationControl() {
    /// tag::log
    const check = new PartiallyOrderedCheckpointTester([
        ['Rejecting anonymous connection attempt.'],
        ['Anonymous connection failed.'],
        ['Rejecting connection attempt from principal not statring with diffusion_.'],
        ['control connection failed.'],
        ['Accepting connection attempt from principal starting with diffusion_.'],
        ['diffusion_control connection established.']
    ]);
    /// end::log
    /// tag::session_management_authentication_control[]
    // Connect to the server.
    const session1 = await diffusion.connect({
        host: 'localhost',
        port: 8080,
        principal: 'control',
        credentials: 'password'
    });

    const authenticator = {
        authenticate: (principal, credentials, sessionProperties, proposedPorperties, callback) => {
            if (principal === '') {
                console.log('Anonymous connection attempt detected. Session establishment rejected.');
                /// tag::log
                check.log('Rejecting anonymous connection attempt.');
                /// end::log
                callback.deny();
            } else if (principal.startsWith('diffusion_')) {
                console.log('Principal begins with diffusion_ prefix. Session establishment accepted.');
                /// tag::log
                check.log('Accepting connection attempt from principal starting with diffusion_.');
                /// end::log
                callback.allow();
            } else {
                console.log('Principal does not begin with diffusion_ prefix. Session establishment rejected.');
                /// tag::log
                check.log('Rejecting connection attempt from principal not statring with diffusion_.');
                /// end::log
                callback.deny();
            }
        },
        onClose: () => {},
        onError: () => {}
    };
    await session1.security.setAuthenticator('before-system-handler', authenticator);

    let session2 = undefined;
    try {
        // Connect to the server.
        session2 = await diffusion.connect({
            host: 'localhost',
            port: 8080
        });
    } catch (err) {
        console.error('Connection could not be established. (Expected)', err);
        /// tag::log
        check.log('Anonymous connection failed.');
        /// end::log
    }

    let session3 = undefined;
    try {
        // Connect to the server.
        session3 = await diffusion.connect({
            host: 'localhost',
            port: 8080,
            principal: 'control',
            credentials: 'password'
        });
    } catch (err) {
        console.error('Connection could not be established. (Expected)', err);
        /// tag::log
        check.log('control connection failed.');
        /// end::log
    }

    // Connect to the server.
    const session4 = await diffusion.connect({
        host: 'localhost',
        port: 8080,
        principal: 'diffusion_control',
        credentials: 'password'
    });
    /// tag::log
    check.log('diffusion_control connection established.');
    /// end::log

    await session1.closeSession();
    await session2?.closeSession();
    await session3?.closeSession();
    await session4.closeSession();
    /// end::session_management_authentication_control[]
    /// tag::log
    await check.done();
    /// end::log
}
