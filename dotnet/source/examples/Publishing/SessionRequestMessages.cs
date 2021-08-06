/**
 * Copyright © 2018, 2021 Push Technology Ltd.
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
using PushTechnology.ClientInterface.Client.Session;
using static System.Console;
using static PushTechnology.ClientInterface.Examples.Runner.Program;

namespace PushTechnology.ClientInterface.Example.Publishing {
    /// <summary>
    /// Control client implementation that sends request messages to a filter and if it gets response displays it on the
    /// system console, and sends another request directly to the session.
    /// </summary>
    public sealed class SendingSessionRequestMessages : IExample {
        private readonly string messagingPath = ">random/requestResponse";

        /// <summary>
        /// Runs the client sending request/response messages example.
        /// </summary>
        /// <param name="cancellationToken">A token used to end the client example.</param>
        /// <param name="args">A single string should be used for the server url.</param>
        public async Task Run( CancellationToken cancellationToken, string[] args ) {
            string serverUrl = args[ 0 ];
            var session = Diffusion.Sessions.Principal( "control" ).Password( "password" )
                .CertificateValidation((cert, chain, errors) => CertificateValidationResult.ACCEPT)
                .Open(serverUrl);

            var messaging = session.Messaging;
            var requestCallback = new RequestCallback();

            // Filter messaging is used to get the session ID for this example
            int requestsSent = await messaging.SendRequestToFilterAsync(
                "$Principal EQ 'client'",
                messagingPath,
                "Hello?",
                requestCallback,
                cancellationToken);

            await Task.Delay(TimeSpan.FromMilliseconds(1000));

            while ( !cancellationToken.IsCancellationRequested ) {
                // Send message to a session using obtained session ID
                string response = await messaging.SendRequestAsync<string, string>(
                    requestCallback.SessionId, messagingPath, "Time", cancellationToken);
                WriteLine($"Received response: '{response}'.");

                await Task.Delay( TimeSpan.FromMilliseconds( 1000 ) );
            }

            // Close the session
            session.Close();
        }

        /// <summary>
        /// A simple IFilteredRequestCallback implementation that prints confirmation of the actions completed.
        /// </summary>
        private class RequestCallback : IFilteredRequestCallback<string> {
            public ISessionId SessionId { get; private set; }

            /// <summary>
            /// Indicates that the stream was closed.
            /// </summary>
            public void OnClose()
                => WriteLine( "A request handler was closed." );

            /// <summary>
            /// Indicates error received by the callback.
            /// </summary>
            public void OnError( ErrorReason errorReason )
                => WriteLine( $"A request handler has received error: '{errorReason}'." );

            /// <summary>
            /// Indicates that a response message was received.
            /// </summary>
            public void OnResponse(ISessionId sessionId, string response) => SessionId = sessionId;

            /// <summary>
            /// Indicates that a error response message was received.
            /// </summary>
            public void OnResponseError( ISessionId sessionId, Exception exception )
                => WriteLine( $"Response error received from session {sessionId}: '{exception}'." );
        }
    }
}
