/**
 * Copyright © 2021 - 2023 DiffusionData Ltd.
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
using static PushTechnology.ClientInterface.Examples.Runner.Program;

namespace PushTechnology.ClientInterface.Example.Features
{
    /// <summary>
    /// Client implementation that demonstrates session reconnection strategy.
    /// </summary>
    public sealed class SessionReconnection : IExample
    {
        private int maximumTimeoutDuration = 1000 * 60 * 10;

        private ReconnectionStrategy reconnectionStrategy = new ReconnectionStrategy();
        /// <summary>
        /// Runs the session reconnection example.
        /// </summary>
        /// <param name="cancellationToken">A token used to end the client example.</param>
        /// <param name="args">A single string should be used for the server url.</param>
        public async Task Run(CancellationToken cancellationToken, string[] args)
        {
            string serverUrl = args[0];

            var factory = Diffusion.Sessions;

            var session = Connect(serverUrl, factory);

            if (session != null)
            {
                WriteLine("The session has been created.");
            }

            Thread.Sleep(60000);

            session.Close();
        }

        public ISession Connect(string url, ISessionFactory initialFactory)
        {
            try
            {
                string principal = "control";
                string password = "password";

                var factory = initialFactory
                    .Principal(principal)
                    .Credentials(Diffusion.Credentials.Password(password))
                    .CertificateValidation((cert, chain, errors) => CertificateValidationResult.ACCEPT)
                    .ReconnectionTimeout(maximumTimeoutDuration)
                    .ReconnectionStrategy(reconnectionStrategy)
                    .SessionStateChangedHandler(OnSessionStateChanged);

                return factory.Open(url);
            }
            catch (Exception ex)
            {
                WriteLine($"Session connection error : {ex}.");
            }

            return null;
        }

        private void OnSessionStateChanged(object sender, SessionListenerEventArgs e)
        {
            if (e.NewState == SessionState.RECOVERING_RECONNECT)
            {
                // The session has been disconnected, and has entered
                // recovery state. It is during this state that
                // the reconnect strategy will be called
                WriteLine("The session has been disconnected.");
            }

            if (e.NewState == SessionState.CONNECTED_ACTIVE)
            {
                // The session has connected for the first time, or it has
                // been reconnected.
                reconnectionStrategy.Retries = 0;

                WriteLine("The session has connected.");
            }

            if (e.OldState == SessionState.RECOVERING_RECONNECT)
            {
                // The session has left recovery state. It may either be
                // attempting to reconnect, or the attempt has been aborted;
                // this will be reflected in the newState.
            }

            if (e.NewState == SessionState.CLOSED_BY_CLIENT)
            {
                WriteLine("The session has been closed.");
            }
        }

        private class ReconnectionStrategy : IReconnectionStrategy
        {
            public int Retries { get; set; }

            // Set the maximum interval between reconnect attempts to 60 seconds.
            private long maximumAttemptInterval = 1000 * 60;

            public ReconnectionStrategy() => Retries = 0;

            public async Task PerformReconnection(IReconnectionAttempt reconnection)
            {
                long wait = Math.Min((long)Math.Pow(2, Retries++) * 100L, maximumAttemptInterval);

                Thread.Sleep((int)wait);

                WriteLine("Attempting to reconnect...");

                reconnection.Start();
            }
        }
    }
}
