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

export async function ignoreClientProposedProperty() {
    /// tag::system_authentication_control_ignore_client_proposed_property[]
    // Connect to the server.
    const session1 = await diffusion.connect({
        host: 'localhost',
        port: 8080,
        principal: 'admin',
        credentials: 'password'
    });

    const authenticationScript1 = session1.security.authenticationScriptBuilder()
        .ignoreClientProposedProperty('Rubble')
        .trustClientProposedPropertyMatches('Flintstone', '.*_Flintstone')
        .build();
    await session1.security.updateAuthenticationStore(authenticationScript1);

    const session2 = await diffusion.connect({
        host: 'localhost',
        port: 8080,
        principal: 'control',
        credentials: 'password',
        properties: {
            'Rubble': 'Barney_Rubble'
        }
    });

    const properties2 = await session1.clients.getSessionProperties(
        session2.sessionId,
        diffusion.clients.PropertyKeys.ALL_PROPERTIES
    );
    console.log('Received the following session properties:');
    for (const key of Object.keys(properties2)) {
        console.log(`  ${key}: ${properties2[key]}`);
    }
    /// tag::log
    expect(properties2['Rubble']).toBeUndefined();
    /// end::log

    await session2.closeSession();

    const session3 = await diffusion.connect({
        host: 'localhost',
        port: 8080,
        principal: 'control',
        credentials: 'password',
        properties: {
            'Flintstone': 'Fred_Flintstone'
        }
    });

    const properties3 = await session1.clients.getSessionProperties(
        session3.sessionId,
        diffusion.clients.PropertyKeys.ALL_PROPERTIES
    );
    console.log('Received the following session properties:');
    for (const key of Object.keys(properties3)) {
        console.log(`  ${key}: ${properties3[key]}`);
    }
    /// tag::log
    expect(properties3['Flintstone']).toBe('Fred_Flintstone');
    /// end::log

    await session3.closeSession();

    /// tag::log
    const restoreScript = session1.security.authenticationScriptBuilder()
        .ignoreClientProposedProperty('Flintstone')
        .build();
    await session1.security.updateAuthenticationStore(restoreScript);
    /// end::log
    await session1.closeSession();
    /// end::system_authentication_control_ignore_client_proposed_property[]
}
