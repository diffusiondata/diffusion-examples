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
using PushTechnology.ClientInterface.Client.Callbacks;
using PushTechnology.ClientInterface.Client.Factories;
using PushTechnology.ClientInterface.Client.Features;
using static System.Console;
using static PushTechnology.ClientInterface.Examples.Program;

namespace PushTechnology.ClientInterface.Examples.Messaging
{
    public sealed class MessageToSessionID : Example {
        public override async Task Run( CancellationToken cancellationToken, string[] args ) 
        {
            string serverUrl = args[0];

            var session = Diffusion.Sessions
                .Principal("admin")
                .Credentials(Diffusion.Credentials.Password("password"))
                .Open(serverUrl);

            string path = "my/message/path";

            var requestStream = new SimpleRequestStream();
            session.Messaging.SetRequestStream(path, requestStream);

            var session2 = Diffusion.Sessions
                .Principal("admin")
                .Credentials(Diffusion.Credentials.Password("password"))
                .Open(serverUrl);

            var requestStream2 = new AnotherRequestStream();
            session2.Messaging.SetRequestStream(path, requestStream2);

            var session3 = Diffusion.Sessions
                .Principal("admin")
                .Credentials(Diffusion.Credentials.Password("password"))
                .Open(serverUrl);

            string response = await session3.Messaging.SendRequestAsync<string, string>(
                session.SessionId, path, "Hello", cancellationToken);
            WriteLine($"Received response: {response}.");

            session.Close();
            session2.Close();
            session3.Close();
        }

        private class SimpleRequestStream : IRequestStream<string, string>
        {
            public void OnClose() {}

            public void OnError(ErrorReason errorReason) {}

            public void OnRequest( string path, string request, IResponder<string> responder ) {
                WriteLine( $"Received message: {request}." );

                responder.Respond( "Goodbye" );
            }
        }

        private class AnotherRequestStream : IRequestStream<string, string>
        {
            public void OnClose() {}

            public void OnError(ErrorReason errorReason) {}

            public void OnRequest(string path, string request, IResponder<string> responder)
            {
                WriteLine($"Received message: {request}.");

                responder.Respond("I'm not supposed to receive a message.");
            }
        }
    }
}
