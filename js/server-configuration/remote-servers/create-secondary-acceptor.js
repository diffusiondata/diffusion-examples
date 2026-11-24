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

export async function remoteServersCreateSecondaryAcceptor() {
    // Connect to the server.
    const session = await diffusion.connect({
        host: 'localhost',
        port: 8080,
        principal: 'admin',
        credentials: 'password'
    });

    const definition = diffusion.newRemoteServerBuilder(diffusion.RemoteServerType.SECONDARY_ACCEPTOR)
        .principal('')
        .credentials(null)
        .connectionOptions({
            [diffusion.ConnectionOption.WRITE_TIMEOUT] : '2000',
            [diffusion.ConnectionOption.MAXIMUM_QUEUE_SIZE]: '1000',
            [diffusion.ConnectionOption.CONNECTION_TIMEOUT]: '15000'
        })
        .missingTopicNotificationFilter('?abc')
        .build('Remote Server 1', 'ws://new.server.url.com');
    await session.remoteServers.createRemoteServer(definition);

    // Clean up
    await session.remoteServers.removeRemoteServer('Remote Server 1');
    await session.closeSession();
}
