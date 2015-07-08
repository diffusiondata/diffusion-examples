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

using PushTechnology.ClientInterface.Client.Factories;
using PushTechnology.ClientInterface.Client.Features;
using PushTechnology.ClientInterface.Client.Session;

namespace UCIStack.Examples
{
    /// <summary>
    /// This is a simple client example that pings the server and prints out the round-trip time.
    /// 
    /// This uses the <see cref="IPings"/> feature only.
    /// </summary>
    public class ClientUsingPings
    {
        #region Fields

        private readonly ISession session;
        private readonly IPings pings;

        #endregion Fields

        #region Constructor

        public ClientUsingPings()
        {
            session = Diffusion.Sessions.Principal( "client" ).Password( "password" )
                .Open( "ws://diffusion.example.com:80" );

            pings = session.GetPingFeature();
        }

        #endregion Constructor

        #region Public Methods

        /// <summary>
        /// Ping the server.
        /// </summary>
        /// <param name="context">The string to log with round-trip time.</param>
        /// <param name="callback">Used to return the ping reply.</param>
        public void Ping( string context, IPingContextCallback<string> callback )
        {
            pings.PingServer( context, callback );
        }

        /// <summary>
        /// Close the session.
        /// </summary>
        public void Close()
        {
            session.Close();
        }

        #endregion Public Methods
    }
}