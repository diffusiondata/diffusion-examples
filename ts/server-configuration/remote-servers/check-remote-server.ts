/*******************************************************************************
 * Copyright (C) 2024 - 2026 DiffusionData Ltd.
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
    ConnectionState,
    newRemoteServerBuilder,
    RemoteServerStatus,
    RemoteServerType,
    SecondaryInitiatorDefinition,
} from 'diffusion';

export async function remoteServersCheck(): Promise<void> {
    // Connect to the server.
    const session = await connect({
        host: 'localhost',
        port: 8080,
        principal: 'admin',
        credentials: 'password'
    });

    // Describe a remote server's connection state.
    const describeStatus = (status: RemoteServerStatus): string => {
        switch (status.connectionState) {
            case ConnectionState.INACTIVE:
                return 'inactive';
            case ConnectionState.CONNECTED:
                return 'connected';
            case ConnectionState.MISSING:
                return 'missing';
            case ConnectionState.FAILED:
                return `connection failed (${status.failureMessage})`;
            case ConnectionState.RETRYING:
                return `retrying (${status.failureMessage})`;
            default:
                return 'unknown';
        }
    };

    // Query the state of a remote server that has not been created yet.
    const stateBefore = await session.remoteServers.getRemoteServerState('Remote Server 1');
    console.log(`Remote Server 1 before it has been created: ${describeStatus(stateBefore)}`);

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
            [ConnectionOption.RECONNECTION_TIMEOUT] : '60000',
            [ConnectionOption.MAXIMUM_QUEUE_SIZE]: '10000',
            [ConnectionOption.CONNECTION_TIMEOUT]: '5000'
        });
    await session.remoteServers.createRemoteServer(
        builder.build('Remote Server 2', 'ws://another.server.url.com')
    );

    const remoteServers = await session.remoteServers.listRemoteServers();
    for (const remoteServer of remoteServers as SecondaryInitiatorDefinition[]) {
        // checkRemoteServer retries a failed connection before reporting its state
        const status = await session.remoteServers.checkRemoteServer(remoteServer.name);
        // getRemoteServerState only reports the current state
        const state = await session.remoteServers.getRemoteServerState(remoteServer.name);
        console.log(
            `${remoteServer.name} (${remoteServer.url}): ${describeStatus(status)}, state: ${describeStatus(state)}`
        );
    }

    // Clean up
    await session.remoteServers.removeRemoteServer('Remote Server 1');
    await session.remoteServers.removeRemoteServer('Remote Server 2');
    await session.closeSession();
}
