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
/// tag::log
import { PartiallyOrderedCheckpointTester } from '../../../test/util'
/// end::log

export async function getGlobalPermissionsExample(): Promise<void> {
    /// tag::log
    const check = new PartiallyOrderedCheckpointTester([
        [
            'VIEW_SESSION',
            'MODIFY_SESSION',
            'REGISTER_HANDLER',
            'VIEW_SERVER',
            'CONTROL_SERVER',
            'VIEW_SECURITY',
            'MODIFY_SECURITY',
            'READ_TOPIC_VIEWS',
            'MODIFY_TOPIC_VIEWS'
        ]
    ]);
    /// end::log
    /// tag::security_get_global_permissions[]
    // Connect to the server.
    const session = await connect({
        host: 'localhost',
        port: 8080,
        principal: 'admin',
        credentials: 'password'
    });

    const globalPermissions = await session.security.getGlobalPermissions();
    for (const permission of globalPermissions) {
        console.log(permission);
        /// tag::log
        check.log(permission);
        /// end::log
    }

    await session.closeSession();
    /// end::security_get_global_permissions[]
    /// tag::log
    check.done();
    /// end::log
}
