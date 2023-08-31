/*******************************************************************************
 * Copyright (C) 2018 - 2023 DiffusionData Ltd.
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

var diffusion = require('diffusion');

// Session security allows you to change the principal that a session is authenticated as. It also  allows users to
// query and update server-side security and authentication stores, which control users, roles and permissions.
// This enables you to manage the capabilities that any logged in user will have access to.

// Connect to Diffusion with control client credentials
diffusion.connect({ 
    host   : 'diffusion.example.com',
    port   : 443,
    secure : true,
    principal : 'control',
    credentials : 'password'
}).then(function(session) {

    // 1. A session change their principal by re-authenticating
    session.security.changePrincipal('admin', 'password').then(function() {
        console.log('Authenticated as admin');
    });
    
    // 2. The security configuration provides details about roles and their assigned permissions
    session.security.getSecurityConfiguration().then(function(config) {
        console.log('Roles for anonymous sessions: ', config.anonymous);
        console.log('Roles for named sessions: ', config.named);
        console.log('Available roles: ', config.roles);
    }, function(error) {
        console.log('Unable to fetch security configuration', error);
    });

    // 3. Changes to the security configuration are done with a SecurityScriptBuilder
    var securityScriptBuilder = session.security.securityScriptBuilder();
   
    // Set the permissions for a particular role - global and topic-scoped
    // Each method on a script builder returns a new builder
    var setPermissionScript = securityScriptBuilder.setGlobalPermissions('SUPERUSER', ['REGISTER_HANDLER'])
                                                   .setTopicPermissions('SUPERUSER', '/foo', ['UPDATE_TOPIC'])
                                                   .build();

    // Update the server-side store with the generated script
    session.security.updateSecurityStore(setPermissionScript).then(function() {
        console.log('Security configuration updated successfully');
    }, function(error) {
        console.log('Failed to update security configuration: ', error);
    });

    // 4. The system authentication configuration lists all users & roles
    session.security.getSystemAuthenticationConfiguration().then(function(config) {
        console.log('System principals: ', config.principals);
        console.log('Anonymous sessions: ', config.anonymous);
    }, function(error) {
        console.log('Unable to fetch system authentication configuration', error);
    });

    // 5. Changes to the system authentication config are done with a SystemAuthenticationScriptBuilder 
    var authenticationScriptBuilder = session.security.authenticationScriptBuilder();
    
    // Add a new user and set password & roles.
    var addUserScript = authenticationScriptBuilder.addPrincipal('Superman', 'correcthorsebatterystapler')
                                                   .assignRoles('Superman', ['SUPERUSER'])
                                                   .build();

    // Update the system authentication store
    session.security.updateAuthenticationStore(addUserScript).then(function() {
        console.log('Updated system authentication config');
    }, function(error) {
        console.log('Failed to update system authentication: ', error);
    });
});
