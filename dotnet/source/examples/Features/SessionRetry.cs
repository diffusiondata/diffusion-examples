/**
 * Copyright © 2022 - 2023 DiffusionData Ltd.
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
using System.Threading;
using System.Threading.Tasks;
using PushTechnology.ClientInterface.Client.Factories;
using PushTechnology.ClientInterface.Client.Session;
using static System.Console;
using static PushTechnology.ClientInterface.Examples.Runner.Program;

namespace PushTechnology.ClientInterface.Example.Features
{
    /// <summary>
    /// Client implementation that demonstrates establishing a session with a session retry strategy.
    /// </summary>
    public sealed class SessionRetry : IExample
    {
        public async Task Run(CancellationToken cancellationToken, string[] args)
        {
            var serverUrl = args[0];

            ISession session = null;

            //Create an initial session establishment retry strategy.
            //It will attempt 5 times to connect to the Diffusion server,
            //with 100 milliseconds interval between attempts. 
            var retryStrategy = new RetryStrategy(100, 5);

            try
            {
                session = Diffusion.Sessions
                    .Principal("admin")
                    .Password("password")
                    .CertificateValidation((cert, chain, errors) => CertificateValidationResult.ACCEPT)
                    .InitialRetryStrategy(retryStrategy)
                    .Open(serverUrl);
                WriteLine($"Session established with session id {session.SessionId}.");
            }
            catch (SessionEstablishmentException ex)
            {
                WriteLine($"Failed to open the session : {ex}.");
            }
            finally
            {
                session.Close();
            }
        }
    }
}
