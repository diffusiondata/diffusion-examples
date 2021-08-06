/**
 * Copyright © 2021 Push Technology Ltd.
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
using System.Collections.Generic;
using System.Threading;
using System.Threading.Tasks;
using PushTechnology.ClientInterface.Client.Factories;
using PushTechnology.ClientInterface.Client.Features.Control.Clients;
using PushTechnology.ClientInterface.Client.Session;
using static System.Console;
using static PushTechnology.ClientInterface.Examples.Runner.Program;

namespace PushTechnology.ClientInterface.Example.Features
{
    /// <summary>
    /// Client implementation that manages remote servers.
    /// </summary>
    public sealed class RemoteServers : IExample
    {
        /// <summary>
        /// Runs the remote servers example.
        /// </summary>
        /// <param name="cancellationToken">A token used to end the client example.</param>
        /// <param name="args">A single string should be used for the server url.</param>
        public async Task Run(CancellationToken cancellationToken, string[] args)
        {
            var serverUrl = args[0];
            var session = Diffusion.Sessions.Principal("admin").Password("password")
                .CertificateValidation((cert, chain, errors) => CertificateValidationResult.ACCEPT)
                .Open(serverUrl);

            IRemoteServer server = null;

            var builder = Diffusion.NewRemoteServerBuilder();
            var remoteServer1 = builder
                        .Principal("principal")
                        .Credentials(Diffusion.Credentials.Password("password"))
                        .ConnectionOptions(new Dictionary<RemoteServerConnectionOption, string>()
                                            {
                                            { RemoteServerConnectionOption.RECONNECTION_TIMEOUT, "50000" },
                                            { RemoteServerConnectionOption.CONNECTION_TIMEOUT, "2500" },
                                            { RemoteServerConnectionOption.WRITE_TIMEOUT, "2000" }
                                            })
                        .MissingTopicNotificationFilter("filter")
                        .Create("Server1", "ws://host:8080");

            try
            {
                server = await session.RemoteServers.CreateRemoteServerAsync(remoteServer1);

                WriteLine($"Remote server '{server.Name}' was created.");
            }
            catch (Exception ex)
            {
                WriteLine($"Failed to create remote server : {ex}.");
                session.Close();
                return;
            }

            try
            {
                var listServers = await session.RemoteServers.ListRemoteServersAsync();

                WriteLine($"The following remote servers exist:");

                foreach (var remoteServer in listServers)
                {
                    WriteLine($"Name: '{remoteServer.Name}', Url: '{remoteServer.ServerUrl}', Principal: '{remoteServer.Principal}', Missing Topic Notification Filter: '{remoteServer.MissingTopicNotificationFilter}'");

                    foreach(var connectionOption in remoteServer.ConnectionOptions)
                    {
                        WriteLine($"Connection Option: '{connectionOption.Key}', Value: '{connectionOption.Value}'");
                    }
                }
            }
            catch (Exception ex)
            {
                WriteLine($"Failed to list remote servers : {ex}.");
                session.Close();
                return;
            }

            try
            {
                var result = await session.RemoteServers.CheckRemoteServerAsync(server.Name);

                WriteLine($"Checking '{server.Name}':");

                WriteLine($"Connection state: '{result.ConnectionState}'");
                WriteLine($"Failure message: '{result.FailureMessage}'");
            }
            catch (Exception ex)
            {
                WriteLine($"Failed to check remote server '{server.Name}' : {ex}.");
                session.Close();
                return;
            }

            try
            {
                await session.RemoteServers.RemoveRemoteServerAsync(server.Name);

                WriteLine($"Remote server '{server.Name}' has been removed.");
            }
            catch (Exception ex)
            {
                WriteLine($"Failed to remove remote server '{server.Name}' : {ex}.");
                session.Close();
                return;
            }

            // Close the session
            session.Close();
        }
    }
}