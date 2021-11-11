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
using static System.Console;
using static PushTechnology.ClientInterface.Examples.Runner.Program;

namespace PushTechnology.ClientInterface.Example.Consuming {
    /// <summary>
    /// Client implementation that listens for messages on a topic path.
    /// </summary>
    public sealed class ReceivingSessionRequestMessages : IExample {
        /// <summary>
        /// Runs the client receiving messages example.
        /// </summary>
        /// <param name="cancellationToken">A token used to end the client example.</param>
        /// <param name="args">A single string should be used for the server url.</param>
        public async Task Run( CancellationToken cancellationToken, string[] args ) {
            string serverUrl = args[ 0 ];
            var session = Diffusion.Sessions.Principal( "client" ).Password( "password" ).Open( serverUrl );
            var messaging = session.Messaging;
            string messagingPath = ">random/requestResponse";

            var requestStream = new SimpleRequestStream();
            messaging.SetRequestStream( messagingPath, requestStream );

            try {
                await Task.Delay( Timeout.InfiniteTimeSpan, cancellationToken );
            } finally {
                // Close session
                messaging.RemoveRequestStream( messagingPath );
                session.Close();
            }
        }

        /// <summary>
        /// A simple IRequestStream implementation that prints confirmation of the actions completed.
        /// </summary>
        private class SimpleRequestStream : IRequestStream<string, string> {
            /// <summary>
            /// Indicates that the request stream was closed.
            /// </summary>
            public void OnClose()
                => WriteLine( "A request handler was closed." );

            /// <summary>
            /// Indicates that the request stream has received error.
            /// </summary>
            public void OnError( ErrorReason errorReason )
                => WriteLine( $"A request handler has received error: {errorReason}." );

            /// <summary>
            /// Indicates that a request was received and responds to it.
            /// </summary>
            /// <remarks>On invalid request you would call: <see cref="IResponder{TResponse}.Reject(string)"/>.</remarks>
            public void OnRequest( string path, string request, IResponder<string> responder ) {
                if ( request == "Hello?" ) {    // message to the filter to obtain the session ID
                    responder.Respond( "Yes" );
                } else {
                    WriteLine( $"Received request: '{request}'." );
                    responder.Respond( DateTime.UtcNow.ToLongTimeString() );
                }
            }
        }
    }
}
