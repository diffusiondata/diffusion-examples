/**
 * Copyright © 2024 Diffusion Data Ltd.
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
using PushTechnology.ClientInterface.Client.Callbacks;
using PushTechnology.ClientInterface.Client.Factories;
using PushTechnology.ClientInterface.Client.Features.Control.Clients;
using PushTechnology.ClientInterface.Client.Session;
using static System.Console;
using static PushTechnology.ClientInterface.Examples.Program;

namespace PushTechnology.ClientInterface.Examples.Monitoring
{
    public sealed class SessionEventListener : Example
    {
        public override async Task Run(CancellationToken cancellationToken, string[] args)
        {
            string serverUrl = args[0];

            var session1 = Diffusion.Sessions
                .Principal("admin")
                .Credentials(Diffusion.Credentials.Password("password"))
                .Open(serverUrl);

            var session2 = Diffusion.Sessions
                .Principal("client")
                .Credentials(Diffusion.Credentials.Password("password"))
                .Open(serverUrl);

            var clientControl = session1.ClientControl;

            // We specify the session properties to be returned and we
            // exclude sessions with the 'admin' principal
            var parameters = Diffusion.NewSessionEventParametersBuilder()
                .Properties(SessionProperty.ALL_FIXED_PROPERTIES)
                .Filter("$Principal NE 'admin'")
                .Build();

            var myEventStream = new MyEventStream();
            var registration = await clientControl.AddSessionEventListenerAsync(myEventStream, parameters);

            await clientControl.SetSessionPropertiesAsync(session2.SessionId, 
                new Dictionary<string, string> { { "$Country", "CA" } });

            session2.Close();
            session1.Close();
        }

        private class MyEventStream : ISessionEventStream
        {
            public void OnClose() 
            {
                WriteLine("Stream closed");
            }

            public void OnError(ErrorReason errorReason) => WriteLine($"An error occured:{errorReason}");

            public void OnSessionEvent(ISessionEventStreamEvent sessionEventStreamEvent) 
            {
                if (sessionEventStreamEvent.IsOpenEvent) 
                {
                    WriteLine($"New session: id={sessionEventStreamEvent.SessionId}");
                    return;
                }

                if (sessionEventStreamEvent.SessionEventStreamEventType == SessionEventStreamEventType.STATE) 
                {
                    WriteLine($"Session state changed: id={sessionEventStreamEvent.SessionId}, state={sessionEventStreamEvent.SessionEventStreamEventState}");
                }
                else 
                {
                    WriteLine($"Session properties changed: id={sessionEventStreamEvent.SessionId}, properties: ");
                    var properties = sessionEventStreamEvent.Properties;
                    foreach (var changedProperty in sessionEventStreamEvent.ChangedProperties)
                    {
                        string line = $"{changedProperty.Key} changed from '{changedProperty.Value}' to '{properties[changedProperty.Key]}'";
                        WriteLine(line);
                    }
                }
            }
        }
    }
}
