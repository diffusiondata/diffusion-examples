/**
 * Copyright © 2016, 2017 Push Technology Ltd.
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
using PushTechnology.ClientInterface.Client.Session;

using static System.Console;
using static PushTechnology.ClientInterface.Examples.Runner.Program;

namespace PushTechnology.ClientInterface.Example.Features {
    /// <summary>
    /// This example shows how a client session is able to ping the server.
    /// </summary>
    public sealed class PingServer : IExample {
        /// <summary>
        /// Runs the server ping example.
        /// </summary>
        /// <param name="cancellationToken">The cancellation token to cancel the current example run.</param>
        /// <param name="args">The optional example arguments.</param>
        public async Task Run( CancellationToken cancellationToken, string[] args ) {
            var serverUrl = args[ 0 ];

            // Connect anonymously
            var session = Diffusion.Sessions.Open( serverUrl );

            // Access the pings feature
            var pings = session.Ping;

            // Loop until cancelled
            while ( !cancellationToken.IsCancellationRequested ) {
                // Ping server roughly every second
                await Task.Delay( TimeSpan.FromSeconds( 1 ) );

                try {
                    // Ping the server. A overload without a cancellation token is available.
                    var details = await pings.PingServerAsync( cancellationToken );

                    // Print out details
                    WriteLine( $"Pinged server at {details.Timestamp}. Received answer after {details.RoundTripTimeSpan}." );

                } catch ( SessionClosedException ) {
                    WriteLine( "Ping failed due to a closed session." );

                } catch ( SessionException ) {
                    WriteLine( "Ping failed due to a communication failure." );

                } catch ( TaskCanceledException ) {
                    WriteLine( "Ping failed due to a manual cancellation." );
                }
            }

            session.Close();
        }
    }
}
