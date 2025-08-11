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
using System.Threading.Tasks;
using System.Threading;
using PushTechnology.ClientInterface.Client.Factories;
using static System.Console;
using static PushTechnology.ClientInterface.Examples.Program;
using PushTechnology.ClientInterface.Client.Features.Control.Clients;
using System.Collections.Generic;

namespace PushTechnology.ClientInterface.Examples.ServerConfiguration.RemoteServers
{
    public sealed class CheckRemoteServers : Example
    {
        public override async Task Run(CancellationToken cancellationToken, string[] args)
        {
            string serverUrl = args[0];

            var session = Diffusion.Sessions
                .Principal("admin")
                .Credentials(Diffusion.Credentials.Password("password"))
                .Open(serverUrl);

            IRemoteServer server = null, server2 = null;

            var builder = (ISecondaryInitiatorBuilder)Diffusion<ISecondaryInitiatorBuilder>.NewRemoteServerBuilder(RemoteServerType.SECONDARY_INITIATOR);

            var initiator = builder
                .Principal("admin")
                .Credentials(Diffusion.Credentials.Password("password"))
                .ConnectionOptions(new Dictionary<RemoteServerConnectionOption, string>()
                                    {
                                        { RemoteServerConnectionOption.RECONNECTION_TIMEOUT, "120000" },
                                        { RemoteServerConnectionOption.CONNECTION_TIMEOUT, "15000" },
                                        { RemoteServerConnectionOption.MAXIMUM_QUEUE_SIZE, "1000" }
                                    })
                .Build("Remote Server 1", "ws://new.server.url.com");

            server = await session.RemoteServers.CreateRemoteServerAsync(initiator, cancellationToken);

            builder = builder.Reset();

            initiator = builder
                .Principal("control")
                .Credentials(Diffusion.Credentials.Password("password"))
                .ConnectionOptions(new Dictionary<RemoteServerConnectionOption, string>()
                                    {
                                        { RemoteServerConnectionOption.RECONNECTION_TIMEOUT, "60000" },
                                        { RemoteServerConnectionOption.CONNECTION_TIMEOUT, "5000" },
                                        { RemoteServerConnectionOption.MAXIMUM_QUEUE_SIZE, "10000" }
                                    })
                .Build("Remote Server 2", "ws://another.server.url.com");

            server2 = await session.RemoteServers.CreateRemoteServerAsync(initiator, cancellationToken);

            var listServers = await session.RemoteServers.ListRemoteServersAsync(cancellationToken);

            foreach (var remoteServer in listServers)
            {
                var result = await session.RemoteServers.CheckRemoteServerAsync(remoteServer.Name, cancellationToken);

                WriteLine($"{remoteServer.Name} ({remoteServer.ServerUrl}): {result.ConnectionState} ({result.FailureMessage})");
            }

            session.Close();
        }
    }
}
