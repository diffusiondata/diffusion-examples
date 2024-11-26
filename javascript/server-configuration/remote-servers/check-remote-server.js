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

export async function remoteServersCheck() {
    // Connect to the server.
    const session = await diffusion.connect({
        host: 'localhost',
        port: 8080,
        principal: 'admin',
        credentials: 'password'
    });

    const builder = diffusion.newRemoteServerBuilder(diffusion.RemoteServerType.SECONDARY_INITIATOR)
        .principal('admin')
        .credentials('password')
        .connectionOptions({
            [diffusion.ConnectionOption.RECONNECTION_TIMEOUT] : '120000',
            [diffusion.ConnectionOption.MAXIMUM_QUEUE_SIZE]: '1000',
            [diffusion.ConnectionOption.CONNECTION_TIMEOUT]: '15000'
        })
        .missingTopicNotificationFilter('?abc');
    await session.remoteServers.createRemoteServer(
        builder.build('Remote Server 1', 'ws://new.server.url.com')
    );
    builder.reset()
        .principal('control')
        .credentials('password')
        .connectionOptions({
            [diffusion.ConnectionOption.RECONNECTION_TIMEOUT] : '60000',
            [diffusion.ConnectionOption.MAXIMUM_QUEUE_SIZE]: '10000',
            [diffusion.ConnectionOption.CONNECTION_TIMEOUT]: '5000'
        });
    await session.remoteServers.createRemoteServer(
        builder.build('Remote Server 2', 'ws://another.server.url.com')
    );

    const remoteServers = await session.remoteServers.listRemoteServers();
    for (const remoteServer of remoteServers) {
        const status = await session.remoteServers.checkRemoteServer(remoteServer.name);
        let statusMessage = '';
        switch (status.connectionState) {
            case diffusion.ConnectionState.INACTIVE:
                statusMessage = 'inactive';
                break;
            case diffusion.ConnectionState.CONNECTED:
                statusMessage = 'connected';
                break;
            case diffusion.ConnectionState.MISSING:
                statusMessage = 'missing';
                break;
            case diffusion.ConnectionState.FAILED:
                statusMessage = `connection failed (${status.failureMessage})`;
                break;
            case diffusion.ConnectionState.RETRYING:
                statusMessage = `retrying (${status.failureMessage})`;
                break;
        }
        console.log(`${remoteServer.name} (${remoteServer.url}): ${statusMessage}`);
    }

    // Clean up
    await session.remoteServers.removeRemoteServer('Remote Server 1');
    await session.remoteServers.removeRemoteServer('Remote Server 2');
    await session.closeSession();
}
