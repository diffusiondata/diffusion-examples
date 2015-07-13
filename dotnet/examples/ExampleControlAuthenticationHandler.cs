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
using PushTechnology.ClientInterface.Client.Details;
using PushTechnology.ClientInterface.Client.Features;
using PushTechnology.ClientInterface.Client.Features.Control.Clients;
using PushTechnology.ClientInterface.Client.Security.Authentication;
using PushTechnology.DiffusionCore.Client.Types;

namespace Examples
{
    /// <summary>
    /// Implementation of <see cref="IControlAuthenticationHandler"/>.
    /// </summary>
    public class ExampleControlAuthenticationHandler : IControlAuthenticationHandler
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
        public void Authenticate( string principal, ICredentials credentials, ISessionDetails sessionDetails,
            IAuthenticationHandlerCallback callback )
        {
            var output = string.Format(
                "ExampleControlAuthenticationHandler asked to authenticate: " +
                "Principal {0}, Credentials {1} -> {2}, SessionDetails {3}: ",
                principal,
                credentials,
                System.Text.Encoding.UTF8.GetString( credentials.ToBytes() ),
                sessionDetails );

            Console.WriteLine( output );

            callback.Abstain();
        }

        /// <summary>
        /// Called when the handler has been registered at the server and is now active.
        /// 
        /// A session can register at most one a single handler of each type.  If there is already a handler registered
        /// for the topic path the operation will fail, the registered handler will be closed, and the session error
        /// handler will be notified.  To change the handler, first close the previous handler.
        /// </summary>
        public void OnActive( IRegisteredHandler registeredHandler )
        {
        }

        /// <summary>
        /// Called if the handler is closed.  This happens if the call to register the handler fails, or the handler is
        /// unregistered.
        /// </summary>
        public void OnClose()
        {
        }
    }
}