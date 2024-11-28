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

export async function sessionStateListenerExample() {
    /// tag::log
    const check = new PartiallyOrderedCheckpointTester([
        ['close']
    ]);
    /// end::log
    /// tag::connection_session_state_listener[]
    // Connect to the server.
    const session = await diffusion.connect({
        host: 'localhost',
        port: 8080,
        principal: 'admin',
        credentials: 'password'
    });

    // Attach state listeners
    session.on({
        disconnect : () => {
            console.log('State changed to disconnected');
            /// tag::log
            check.log('disconnect');
            /// end::log
        },
        reconnect : () => {
            console.log('State changed to reconnected');
            /// tag::log
            check.log('reconnect');
            /// end::log
        },
        error : (error) => {
            console.log('An error occured', error);
            /// tag::log
            check.log('error');
            /// end::log
        },
        close : (reason) => {
            console.log('State changed to closed', reason);
            /// tag::log
            check.log('close');
            /// end::log
        }
    });

    // Insert work here

    await session.closeSession();
    /// end::connection_session_state_listener[]
    /// tag::log
    check.done();
    /// end::log
}
