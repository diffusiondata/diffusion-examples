/**
 * Copyright © 2014, 2016 Push Technology Ltd.
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
 */

using System.Collections.Generic;
using System.Linq;
using PushTechnology.ClientInterface.Client.Callbacks;
using PushTechnology.ClientInterface.Client.Factories;
using PushTechnology.ClientInterface.Client.Features.Control.Clients.SecurityControl;
using PushTechnology.ClientInterface.Client.Types;

namespace Examples {
    /// <summary>
    /// An example of using a control client to alter the security configuration.
    ///
    /// This uses the <see cref="ISecurityControl"/> feature only.
    /// </summary>
    public class ControlClientChangingSecurity {
        private readonly ISecurityControl securityControl;

        public ControlClientChangingSecurity() {
            // Authenticate with a user that has the VIEW_SECURITY and MODIFY_SECURITY permissions.
            var session = Diffusion.Sessions.Principal( "admin" ).Password( "password" )
                // Use a secure channel because we're transferring sensitive information.
                .Open( "wss://diffusion.example.com:8080" );

            securityControl = session.SecurityControl;
        }

        public void DoCapitalizeRoles( IUpdateStoreCallback callback ) {
            securityControl.GetSecurity( new CapitalizeRoles( securityControl, callback ) );
        }

        private class CapitalizeRoles : IConfigurationCallback {
            private readonly ISecurityControl theSecurityControl;
            private readonly IUpdateStoreCallback theCallback;

            /// <summary>
            /// Constructor.
            /// </summary>
            /// <param name="securityControl">The security control object.</param>
            /// <param name="callback">The callback object.</param>
            public CapitalizeRoles( ISecurityControl securityControl, IUpdateStoreCallback callback ) {
                theSecurityControl = securityControl;
                theCallback = callback;
            }

            /// <summary>
            /// Notification of a contextual error related to this callback. This is analogous to an exception being
            /// raised. Situations in which <code>OnError</code> is called include the session being closed, a
            /// communication timeout, or a problem with the provided parameters. No further calls will be made to this
            /// callback.
            /// </summary>
            /// <param name="errorReason">errorReason a value representing the error; this can be one of constants
            /// defined in <see cref="ErrorReason" />, or a feature-specific reason.</param>
            public void OnError( ErrorReason errorReason ) {
                // This might fail if the session lacks the required permissions.
                theCallback.OnError( errorReason );
            }

            /// <summary>
            /// This is called to return the requested security configuration.
            /// </summary>
            /// <param name="configuration">The snapshot of information from the security store.</param>
            public void OnReply( ISecurityConfiguration configuration ) {
                var builder = theSecurityControl.ScriptBuilder();

                builder = builder.SetRolesForAnonymousSessions(
                    Capitalize( configuration.RolesForAnonymousSessions ) );

                builder = builder.SetRolesForNamedSessions(
                    Capitalize( configuration.RolesForNamedSessions ) );

                foreach ( var role in configuration.Roles ) {
                    var oldName = role.Name;
                    var newName = Capitalize( oldName );

                    // Only if new name is different
                    if ( !oldName.Equals( newName ) ) {
                        // Global permissions
                        var globalPermissions = role.GlobalPermissions;

                        if ( globalPermissions.Count > 0 ) {
                            // Remove global permissions for old role
                            builder = builder.SetGlobalPermissions( oldName, new List<GlobalPermission>() );

                            // Set global permissions for new role
                            builder = builder.SetGlobalPermissions( newName,
                                new List<GlobalPermission>( role.GlobalPermissions ) );
                        }

                        var defaultTopicPermissions = role.DefaultTopicPermissions;

                        if ( defaultTopicPermissions.Count > 0 ) {
                            // Remove default topic permissions for old role
                            builder = builder.SetDefaultTopicPermissions( oldName, new List<TopicPermission>() );

                            // Set default topic permissions for new role
                            builder = builder.SetDefaultTopicPermissions( newName,
                                new List<TopicPermission>( role.DefaultTopicPermissions ) );
                        }

                        var topicPermissions = role.TopicPermissions;

                        if ( topicPermissions.Count > 0 ) {
                            foreach ( var entry in topicPermissions ) {
                                var topicPath = entry.Key;

                                // Remove old topic permissions
                                builder = builder.RemoveTopicPermissions( oldName, topicPath );

                                // Set new topic permissions
                                builder = builder.SetTopicPermissions( newName, topicPath, entry.Value );
                            }
                        }
                    }

                    var oldIncludedRoles = role.IncludedRoles;

                    if ( oldIncludedRoles.Count > 0 ) {
                        // Remove old included roles
                        builder = builder.SetRoleIncludes( oldName, new List<string>() );
                    }

                    // This is done even if role name did not change as it is possible that roles included may have
                    var newIncludedRoles = Capitalize( oldIncludedRoles );

                    builder = builder.SetRoleIncludes( newName, newIncludedRoles );
                }
            }

            private static List<string> Capitalize( IEnumerable<string> roles ) {
                return roles.Select( Capitalize ).ToList();
            }

            private static string Capitalize( string role ) {
                return char.ToUpper( role[ 0 ] ) + role.Substring( 1 );
            }
        }
    }
}