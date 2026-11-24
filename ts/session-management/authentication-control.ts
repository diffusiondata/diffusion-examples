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
    Authenticator,
    AuthenticatorCallback,
    connect,
    Credentials,
    Session,
    SessionProperties
} from 'diffusion';

export async function sessionManagementAuthenticationControl(): Promise<void> {
    // Connect to the server.
    const session1 = await connect({
        host: 'localhost',
        port: 8080,
        principal: 'control',
        credentials: 'password'
    });

    const authenticator: Authenticator = {
        authenticate: (
            principal: string,
            credentials: Credentials,
            sessionProperties: SessionProperties,
            proposedPorperties: SessionProperties,
            callback: AuthenticatorCallback
        ) => {
            if (principal === '') {
                console.log('Anonymous connection attempt detected. Session establishment rejected.');
                callback.deny();
            } else if (principal.startsWith('diffusion_')) {
                console.log('Principal begins with diffusion_ prefix. Session establishment accepted.');
                callback.allow();
            } else {
                console.log('Principal does not begin with diffusion_ prefix. Session establishment rejected.');
                callback.deny();
            }
        },
        onClose: () => {},
        onError: () => {}
    };
    await session1.security.setAuthenticator('before-system-handler', authenticator);

    let session2: Session | undefined = undefined;
    try {
        // Connect to the server.
        session2 = await connect({
            host: 'localhost',
            port: 8080
        });
    } catch (err) {
        console.error('Connection could not be established. (Expected)', err);
    }

    let session3: Session | undefined = undefined;
    try {
        // Connect to the server.
        session3 = await connect({
            host: 'localhost',
            port: 8080,
            principal: 'control',
            credentials: 'password'
        });
    } catch (err) {
        console.error('Connection could not be established. (Expected)', err);
    }

    // Connect to the server.
    const session4 = await connect({
        host: 'localhost',
        port: 8080,
        principal: 'diffusion_control',
        credentials: 'password'
    });

    await session1.closeSession();
    await session2?.closeSession();
    await session3?.closeSession();
    await session4.closeSession();
}
