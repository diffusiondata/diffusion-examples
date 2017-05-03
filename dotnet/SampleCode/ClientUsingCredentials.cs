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

using PushTechnology.ClientInterface.Client.Factories;
using PushTechnology.ClientInterface.Client.Features;
using PushTechnology.ClientInterface.Client.Session;

namespace Examples {
    /// <summary>
    /// This demonstrates a client's use of credentials, specifically the ability to change the principal for an active
    /// session.
    ///
    /// This is not a realistic use case on its own, but it shown separately here for clarity.
    /// </summary>
    public class ClientUsingCredentials {
        private readonly ISession session;
        private readonly ISecurity security;

        public ClientUsingCredentials() {
            session = Diffusion.Sessions.Principal( "client" ).Password( "password" )
                .Open( "ws://diffusion.example.com:80" );

            security = session.GetSecurityFeature();
        }

        /// <summary>
        /// Request a change of principal for the session.
        /// </summary>
        /// <param name="principal">The new principal name.</param>
        /// <param name="password">The password.</param>
        /// <param name="callback">Notifies success or failure.</param>
        public void ChangePrincipal( string principal, string password, IChangePrincipalCallback callback ) {
            security.ChangePrincipal( principal, Diffusion.Credentials.Password( password ), callback );
        }

        /// <summary>
        /// Close the session.
        /// </summary>
        public void Close() {
            session.Close();
        }
    }
}