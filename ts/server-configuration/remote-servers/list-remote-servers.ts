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

import {
    connect,
    ConnectionOption,
    newRemoteServerBuilder,
    RemoteServerType,
    SecondaryInitiatorDefinition,
} from 'diffusion';
/// tag::log
import { PartiallyOrderedCheckpointTester } from '../../../../test/util';
/// end::log

export async function remoteServersList(): Promise<void> {
    /// tag::log
    const check = new PartiallyOrderedCheckpointTester([[
        'Remote Server 1 (ws://new.server.url.com)',
        'Remote Server 2 (ws://another.server.url.com)'
    ]]);
    /// end::log
    /// tag::remote_servers_list[]
    // Connect to the server.
    const session = await connect({
        host: 'localhost',
        port: 8080,
        principal: 'admin',
        credentials: 'password'
    });

    const builder = newRemoteServerBuilder(RemoteServerType.SECONDARY_INITIATOR)
        .principal('admin')
        .credentials('password')
        .connectionOptions({
            [ConnectionOption.RECONNECTION_TIMEOUT] : '120000',
            [ConnectionOption.MAXIMUM_QUEUE_SIZE]: '1000',
            [ConnectionOption.CONNECTION_TIMEOUT]: '15000'
        })
        .missingTopicNotificationFilter('?abc');
    await session.remoteServers.createRemoteServer(
        builder.build('Remote Server 1', 'ws://new.server.url.com')
    );
    builder.reset()
        .principal('control')
        .credentials('password')
        .connectionOptions({
            [ConnectionOption.RECONNECTION_TIMEOUT] : '6000',
            [ConnectionOption.MAXIMUM_QUEUE_SIZE]: '10000',
            [ConnectionOption.CONNECTION_TIMEOUT]: '5000'
        });
    await session.remoteServers.createRemoteServer(
        builder.build('Remote Server 2', 'ws://another.server.url.com')
    );

    const remoteServers = await session.remoteServers.listRemoteServers();
    for (const remoteServer of remoteServers as SecondaryInitiatorDefinition[]) {
        console.log(`${remoteServer.name} (${remoteServer.url})`);
        /// tag::log
        check.log(`${remoteServer.name} (${remoteServer.url})`);
        /// end::log
    }

    // Clean up
    await session.remoteServers.removeRemoteServer('Remote Server 1');
    await session.remoteServers.removeRemoteServer('Remote Server 2');
    await session.closeSession();
    /// end::remote_servers_list[]
    /// tag::log
    await check.done();
    /// end::log
}
