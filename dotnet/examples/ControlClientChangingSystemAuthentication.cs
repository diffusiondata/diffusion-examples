/**
 * Copyright © 2014, 2015 Push Technology Ltd.
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
using PushTechnology.ClientInterface.Client.Features.Control.Clients;
using PushTechnology.ClientInterface.Client.Types;
using IUpdateStoreCallback = PushTechnology.ClientInterface.Client.Features.Control.Clients.SecurityControl.IUpdateStoreCallback;

namespace Examples
{
    public class ControlClientChangingSystemAuthentication
    {
        #region Fields

        private readonly ISystemAuthenticationControl theSystemAuthenticationControl;

        #endregion Fields

        #region Constructor

        public ControlClientChangingSystemAuthentication()
        {
            var session =
                Diffusion.Sessions
                    // Authenticate with a user that has the VIEW_SECURITY and MODIFY_SECURITY permissions
                    .Principal( "control" ).Password( "password" )
                    // Use a secure channel because we're transferring sensitive information.
                    .Open( "dpt://localhost:8080" );

            theSystemAuthenticationControl = session.GetSystemAuthenticationControlFeature();

            session.Start();
        }

        #endregion Constructor

        #region Public Methods

        /// <summary>
        /// For all system users, update the assigned roles to replace the 'SUPERUSER' role and with 'ADMINISTRATOR'.
        /// </summary>
        /// <param name="callback"></param>
        public void ChangeSuperUsersToAdministrators( IUpdateStoreCallback callback )
        {
            theSystemAuthenticationControl.GetSystemAuthentication( 
                new ChangeSuperusersToAdministrators( theSystemAuthenticationControl, callback ) );
        }

        #endregion Public Methods

        #region Private Classes

        private class InternalUpdateStoreCallback : IUpdateStoreCallback
        {
            /// <summary>
            /// The script was applied successfully.
            /// </summary>
            public void OnSuccess()
            {
            }

            /// <summary>
            /// The script was rejected.  No changes were made to the system authentication store.
            /// </summary>
            /// <param name="errors">The details of why the script was rejected.</param>
            public void OnRejected( ICollection<IErrorReport> errors )
            {
            }

            /// <summary>
            /// Notification of a contextual error related to this callback. This is
            /// analogous to an exception being raised. Situations in which
            /// <code>OnError</code> is called include the session being closed, a
            /// communication timeout, or a problem with the provided parameters. No
            /// further calls will be made to this callback.
            /// </summary>
            /// <param name="errorReason">errorReason a value representing the error; this can be one of
            /// constants defined in <see cref="ErrorReason" />, or a feature-specific
            /// reason.</param>
            public void OnError( ErrorReason errorReason )
            {
            }
        }

        private class ChangeSuperusersToAdministrators : IConfigurationCallback
        {
            #region Fields

            private readonly ISystemAuthenticationControl theSystemAuthenticationControl;
            private readonly IUpdateStoreCallback theCallback;

            #endregion Fields

            #region Constructor

            public ChangeSuperusersToAdministrators( 
                ISystemAuthenticationControl systemAuthenticationControl, 
                IUpdateStoreCallback callback )
            {
                theSystemAuthenticationControl = systemAuthenticationControl;
                theCallback = callback;
            }

            #endregion Constructor

            /// <summary>
            /// The configuration callback reply.
            /// </summary>
            /// <param name="systemAuthenticationConfiguration">The system authenticationConfiguration stored on the server.</param>
            public void OnReply( ISystemAuthenticationConfiguration systemAuthenticationConfiguration )
            {
                var builder = theSystemAuthenticationControl.ScriptBuilder();

                // For all system users...
                foreach( var principal in systemAuthenticationConfiguration.Principals )
                {
                    var assignedRoles = principal.AssignedRoles;

                    // ...that have the 'SUPERUSER' assigned role...
                    if( !assignedRoles.Contains( "SUPERUSER" ) ) continue;

                    var newRoles = new HashSet<string>( assignedRoles );

                    newRoles.Remove( "SUPERUSER" );
                    newRoles.Add( "ADMINISTRATOR" );

                    // ...and add a command to the script that updates the user's assigned roles, replacing 'SUPERUSER' with
                    // 'ADMINISTRATOR'.
                    builder = builder.AssignRoles( principal.Name, newRoles.ToList() );
                }

                var script = builder.Script();

                theSystemAuthenticationControl.UpdateStore( script, theCallback );
            }

            /// <summary>
            /// Notification of a contextual error related to this callback. This is
            /// analogous to an exception being raised. Situations in which
            /// <code>OnError</code> is called include the session being closed, a
            /// communication timeout, or a problem with the provided parameters. No
            /// further calls will be made to this callback.
            /// </summary>
            /// <param name="errorReason">errorReason a value representing the error; this can be one of
            /// constants defined in <see cref="ErrorReason" />, or a feature-specific
            /// reason.</param>
            public void OnError( ErrorReason errorReason )
            {
                theCallback.OnError( errorReason );
            }
        }

        #endregion Private Classes

        #region Tests

        //[Test]
        public void ControlClientChangingSystemAuthenticationTest()
        {
            var client = new ControlClientChangingSystemAuthentication();
            
            client.ChangeSuperUsersToAdministrators( new InternalUpdateStoreCallback() );
        }

        #endregion Tests
    }
}