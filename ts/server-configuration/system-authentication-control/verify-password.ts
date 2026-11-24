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

export async function verifyPassword(): Promise<void> {
    // Connect to the server.
    const session1 = await connect({
        host: 'localhost',
        port: 8080,
        principal: 'admin',
        credentials: 'password'
    });

    const authenticationScript1 = session1.security.authenticationScriptBuilder()
        .verifyPassword('control', 'password')
        .setPassword('control', '12345')
        .build();
    await session1.security.updateAuthenticationStore(authenticationScript1);

    const session2 = await connect({
        host: 'localhost',
        port: 8080,
        principal: 'control',
        credentials: '12345'
    });
    console.log(`Control session has been established: ${session2.sessionId}`);
    await session2.closeSession();

    const authenticationScript2 = session1.security.authenticationScriptBuilder()
        .verifyPassword('control', 'this_is_not_the_right_password')
        .setPassword('control', 'new_password')
        .build();
    try {
        await session1.security.updateAuthenticationStore(authenticationScript2);
    } catch (err) {
        // expected to fail
        console.log('Password verification failed');
    }

    try {
        await connect({
            host: 'localhost',
            port: 8080,
            principal: 'control',
            credentials: 'new_password'
        });
    }
    catch (err) {
        // expected to fail
        console.error('Could not connect', err.message);
    }

    await session1.closeSession();
}
