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

export async function securityControlRemovePathPermissions(): Promise<void> {
    /// tag::security_control_remove_path_permissions[]
    // Connect to the server.
    const session = await connect({
        host: 'localhost',
        port: 8080,
        principal: 'admin',
        credentials: 'password'
    });

    const securityScript1 = session.security.securityScriptBuilder()
        .setPathPermissions('CLIENT','my/topic/path', [ 'UPDATE_TOPIC', 'MODIFY_TOPIC' ])
        .build();
    console.log('Allowing Role CLIENT to update and modify my/topic/path.');
    console.log(securityScript1);
    await session.security.updateSecurityStore(securityScript1);

    const securityScript2 = session.security.securityScriptBuilder()
        .removePathPermissions('CLIENT','my/topic/path')
        .build();
    console.log('Removing path permissions for Role CLIENT at my/topic/path.');
    console.log(securityScript2);
    await session.security.updateSecurityStore(securityScript2);

    await session.closeSession();
    /// end::security_control_remove_path_permissions[]
}
