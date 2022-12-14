/*******************************************************************************
 * Copyright (C) 2019 - 2022 Push Technology Ltd.
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

import { connect, topics, Session, SecurityConfiguration, SystemAuthenticationConfiguration } from 'diffusion';


// example showcasing how to update the security and authentication stores
export async function securityExample(): Promise<void> {

    // Session security allows you to change the principal that a session is authenticated as. It also  allows users to
    // query and update server-side security and authentication stores, which control users, roles and permissions.
    // This enables you to manage the capabilities that any logged in user will have access to.

    // Connect to Diffusion with control client credentials
    const session: Session = await connect({
        host: 'diffusion.example.com',
        port: 443,
        secure: true,
        principal: 'control',
        credentials: 'password'
    });

    // 1. A session change their principal by re-authenticating
    await session.security.changePrincipal('admin', 'password');
    console.log('Authenticated as admin');

    try {
        // 2. The security configuration provides details about roles and their assigned permissions
        const config: SecurityConfiguration = await session.security.getSecurityConfiguration();
        console.log('Roles for anonymous sessions: ', config.anonymous);
        console.log('Roles for named sessions: ', config.named);
        console.log('Available roles: ', config.roles);
    } catch(error) {
        console.log('Unable to fetch security configuration', error);
    }

    // 3. Changes to the security configuration are done with a SecurityScriptBuilder
    const securityScriptBuilder = session.security.securityScriptBuilder();

    // Set the permissions for a particular role - global and topic-scoped
    // Each method on a script builder returns a new builder
    const setPermissionScript = securityScriptBuilder
        .setGlobalPermissions('SUPERUSER', ['REGISTER_HANDLER'])
        .setPathPermissions('SUPERUSER', '/foo', ['UPDATE_TOPIC'])
        .build();

    try {
        // Update the server-side store with the generated script
        await session.security.updateSecurityStore(setPermissionScript);
        console.log('Security configuration updated successfully');
    } catch(error) {
        console.log('Failed to update security configuration: ', error);
    }

    try {
        // 4. The system authentication configuration lists all users & roles
        const config: SystemAuthenticationConfiguration = await session.security.getSystemAuthenticationConfiguration();
        console.log('System principals: ', config.principals);
        console.log('Anonymous sessions: ', config.anonymous);
    } catch(error) {
        console.log('Unable to fetch system authentication configuration', error);
    }

    // 5. Changes to the system authentication config are done with a SystemAuthenticationScriptBuilder
    const authenticationScriptBuilder = session.security.authenticationScriptBuilder();

    // Add a new user and set password & roles.
    const addUserScript = authenticationScriptBuilder
        .addPrincipal('Superman', 'correcthorsebatterystapler')
        .assignRoles('Superman', ['SUPERUSER'])
        .build();

    try {
        // Update the system authentication store
        await session.security.updateAuthenticationStore(addUserScript);
        console.log('Updated system authentication config');
    } catch(error) {
        console.log('Failed to update system authentication: ', error);
    }
}
