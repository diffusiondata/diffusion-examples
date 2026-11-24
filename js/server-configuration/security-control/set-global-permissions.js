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

export async function securityControlSetGlobalPermissions() {
    // Connect to the server.
    const session = await diffusion.connect({
        host: 'localhost',
        port: 8080,
        principal: 'admin',
        credentials: 'password'
    });

    const securityScript = session.security.securityScriptBuilder()
        .setGlobalPermissions('CLIENT', [ 'VIEW_SERVER', 'VIEW_SESSION' ])
        .build();
    console.log('Adding the following permissions to the global permissions of Role CLIENT: VIEW_SERVER and VIEW_SESSION');
    console.log(securityScript);
    await session.security.updateSecurityStore(securityScript);

    // Clean up
    const securityScriptCleanup = session.security.securityScriptBuilder()
        .setGlobalPermissions('CLIENT', [])
        .build();
    await session.security.updateSecurityStore(securityScriptCleanup);
    await session.closeSession();
}
