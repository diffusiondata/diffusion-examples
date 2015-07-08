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

using System;
using System.Linq;
using System.Text;
using PushTechnology.ClientInterface.Client.Details;
using PushTechnology.ClientInterface.Client.Factories;
using PushTechnology.ClientInterface.Client.Features;
using PushTechnology.ClientInterface.Client.Features.Control.Clients;
using PushTechnology.ClientInterface.Client.Security.Authentication;
using PushTechnology.ClientInterface.Client.Session;
using PushTechnology.DiffusionCore.Client.Types;

namespace UCIStack.Examples
{
    /// <summary>
    /// This demonstrates the use of a control client to authenticate client connections.
    /// 
    /// This uses the <see cref="IAuthenticationControl"/> feature.
    /// </summary>
    public class ControlClientIdentityChecks
    {
        #region Fields

        private readonly ISession session;

        #endregion Fields

        #region Constructor

        /// <summary>
        /// Constructor.
        /// </summary>
        public ControlClientIdentityChecks()
        {
            session = Diffusion.Sessions.Principal( "control" ).Password( "password" )
                .Open( "ws://diffusion.example.com:80" );

            var authenticationControl = session.GetAuthenticationControlFeature();

            // To register the authentication handler, this client session must have the AUTHENTICATE and
            // REGISTER_HANDLER permissions.

            authenticationControl.SetAuthenticationHandler(
                "example-handler",
                Enum.GetValues( typeof( DetailType ) ).OfType<DetailType>().ToList(),
                new Handler() );
        }

        #endregion Constructor

        #region Private Classes

        private class Handler : ServerHandlerDefault, IControlAuthenticationHandler
        {
            /// <summary>
            /// Request authentication.
            /// 
            /// The server calls this to authenticate new sessions, and when a client requests the session principal is
            /// changed.
            /// 
            /// For each call to Authenticate, the authentication handler should respond by calling one of the methods of
            /// the provided callback.  The handler may return immediately and process the authentication request
            /// asynchronously.  The client session will be blocked until a callback method is called.
            /// </summary>
            /// <param name="principal"></param>
            /// <param name="credentials"></param>
            /// <param name="sessionDetails"></param>
            /// <param name="callback"></param>
            public void Authenticate(
                string principal, 
                ICredentials credentials, 
                ISessionDetails sessionDetails,
                IAuthenticationHandlerCallback callback )
            {
                var passwordBytes = Encoding.UTF8.GetBytes( "password" );

                if( "admin".Equals( principal ) &&
                    credentials.Type == CredentialsType.PLAIN_PASSWORD &&
                    credentials.ToBytes().Equals( passwordBytes ) )
                {
                    callback.Allow();
                }
                else
                {
                    callback.Deny();
                }
            }
        }

        private class ServerHandlerDefault : IServerHandler
        {
            /// <summary>
            /// Called when the handler has been registered at the server and is now active.
            /// 
            /// A session can register at most one a single handler of each type.  If there is already a handler registered
            /// for the topic path the operation will fail, the registered handler will be closed, and the session error
            /// handler will be notified.  To change the handler, first close the previous handler.
            /// </summary>
            public virtual void OnActive( IRegisteredHandler registeredHandler )
            {
            }

            /// <summary>
            /// Called if the handler is closed.  This happens if the call to register the handler fails, or the handler is
            /// unregistered.
            /// </summary>
            public virtual void OnClose()
            {
            }
        }

        #endregion Private Classes
    }
}