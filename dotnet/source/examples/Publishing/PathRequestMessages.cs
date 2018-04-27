/**
 * Copyright © 2018 Push Technology Ltd.
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
using System.Threading;
using System.Threading.Tasks;
using PushTechnology.ClientInterface.Client.Factories;
using static System.Console;
using static PushTechnology.ClientInterface.Examples.Runner.Program;

namespace PushTechnology.ClientInterface.Example.Publishing {
    /// <summary>
    /// Control client implementation that sends request messages to a path and if it gets a response it displays it on the
    /// system console.
    /// </summary>
    public sealed class SendingPathRequestMessages : IExample {
        /// <summary>
        /// Runs the client sending request/response messages example.
        /// </summary>
        /// <param name="cancellationToken">A token used to end the client example.</param>
        /// <param name="args">A single string should be used for the server url.</param>
        public async Task Run( CancellationToken cancellationToken, string[] args ) {
            var serverUrl = args[ 0 ];
            var session = Diffusion.Sessions.Principal( "control" ).Password( "password" ).Open( serverUrl );
            var messaging = session.Messaging;
            var messagingPath = ">random/requestResponse";

            while ( !cancellationToken.IsCancellationRequested ) {
                try {
                    string response = await messaging.SendRequestAsync<string, string>(
                        messagingPath, "Time", cancellationToken );
                    WriteLine( $"Received response: '{response}'." );
                } catch ( Exception e ) {
                    WriteLine( $"Got exception: '{e.Message}'." );
                }

                await Task.Delay( TimeSpan.FromMilliseconds( 1000 ) );
            }

            // Close the session
            session.Close();
        }
    }
}
