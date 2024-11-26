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

export async function clientControlChangeRolesViaSessionId() {
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

    const properties1 = await session1.clients.getSessionProperties(
        session2.sessionId,
        ['$Roles']
    );
    console.log(`Original session roles: ${properties1['$Roles']}`);

    await session1.clients.changeRoles(session2.sessionId, [], ['TOPIC_CONTROL']);

    const properties2 = await session1.clients.getSessionProperties(
        session2.sessionId,
        ['$Roles']
    );
    console.log(`Changed session roles: ${properties2['$Roles']}`);

    await session1.closeSession();
    await session2.closeSession();
}
