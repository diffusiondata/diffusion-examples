/**
 * Copyright © 2023 - 2024 Diffusion Data Ltd.
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
using PushTechnology.ClientInterface.Client.Session.Reconnection;
using static System.Console;
using static PushTechnology.ClientInterface.Examples.Program;

namespace PushTechnology.ClientInterface.Examples.Connection.Resilience
{
    public sealed class ReconnectionStrategy : Example
    {
        public override async Task Run(CancellationToken cancellationToken, string[] args)
        {
            string serverUrl = args[0];

            // Set the maximum amount of time the client will try and reconnect for to 10 minutes.
            int maximumTimeoutDuration = 1000 * 60 * 10;
            var reconnectionStrategy = new SessionReconnectionStrategy();

            var session = Diffusion.Sessions
                .Principal("admin")
                .Credentials(Diffusion.Credentials.Password("password"))
                .ReconnectionTimeout(maximumTimeoutDuration)
                .ReconnectionStrategy(reconnectionStrategy)
                .Open(serverUrl);

            WriteLine($"Connected. Session Identifier: {session.SessionId}.");

            // Insert work here...

            session.Close();
        }

        /// <summary>
        /// This class is used to reconnect to the server when the session is disconnected unexpectedly.
        /// </summary>
        private class SessionReconnectionStrategy : IReconnectionStrategy
        {
            public int Retries { get; set; }

            public SessionReconnectionStrategy() => Retries = 0;

            public async Task PerformReconnection(IReconnectionAttempt reconnection)
            {
                if (Retries > 10) 
                {
                    // Abort after 10 attempts.
                    throw new Exception();
                }
                else
                {
                    Retries++;

                    // Retry after 3 seconds.
                    await Task.Delay(TimeSpan.FromMilliseconds(3000));

                    WriteLine("Attempting to reconnect...");
                   
                    reconnection.Start();
                }
            }
        }
    }
}
