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
/// tag::log
import { PartiallyOrderedCheckpointTester } from '../../../../test/util';
/// end::log

export async function clientControlQueueConflationViaSessionFilter(): Promise<void> {
    /// tag::log
    const check = new PartiallyOrderedCheckpointTester([]);
    /// end::log
    /// tag::client_control_queue_conflation_via_session_filter[]
    // Connect to the server.
    const session1 = await connect({
        host: 'localhost',
        port: 8080,
        principal: 'admin',
        credentials: 'password'
    });

    // Connect to the server.
    const session2 = await connect({
        host: 'localhost',
        port: 8080,
        principal: 'client',
        credentials: 'password'
    });

    await session1.clients.setConflated('$Principal is "client"', false);

    await session1.closeSession();
    await session2.closeSession();
    /// end::client_control_queue_conflation_via_session_filter[]
    /// tag::log
    await check.done();
    /// end::log
}
