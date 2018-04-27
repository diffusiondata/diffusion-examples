/**
 * Copyright © 2018 Push Technology Ltd.
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
using PushTechnology.ClientInterface.Client.Content;
using PushTechnology.ClientInterface.Client.Factories;
using PushTechnology.ClientInterface.Client.Features;
using PushTechnology.ClientInterface.Client.Features.Control.Topics;
using PushTechnology.ClientInterface.Client.Session;
using PushTechnology.ClientInterface.Client.Types;
using static System.Console;
using static PushTechnology.ClientInterface.Examples.Runner.Program;

namespace PushTechnology.ClientInterface.Example.Publishing {
    /// <summary>
    /// Client implementation that listens for messages on a topic path.
    /// </summary>
    public sealed class ReceivingMessages : IExample {
        /// <summary>
        /// Runs the client receiving messages example.
        /// </summary>
        /// <param name="cancellationToken">A token used to end the client example.</param>
        /// <param name="args">A single string should be used for the server url.</param>
        public async Task Run( CancellationToken cancellationToken, string[] args ) {
            var serverUrl = args[ 0 ];
            var session = Diffusion.Sessions.Principal( "control" ).Password( "password" ).Open( serverUrl );
            var messagingControl = session.MessagingControl;

            var topicPath = ">random/messages";

            messagingControl.AddMessageHandler( topicPath, new MessageHandler() );

            try {
                await Task.Delay( Timeout.InfiniteTimeSpan, cancellationToken );
            } finally {
                // Close session
                session.Close();
            }
        }

        /// <summary>
        /// A simple IMessageHandler implementation that prints confirmation of the actions completed.
        /// </summary>
        private sealed class MessageHandler : IMessageHandler {
            /// <summary>
            /// Indicates that the message hander was registered, and that it is active.
            /// </summary>
            public void OnActive( string topicPath, IRegisteredHandler registeredHandler )
                => WriteLine( $"A message handler is registered for {topicPath}." );

            /// <summary>
            /// Indicates that the message hander was closed.
            /// </summary>
            public void OnClose( string topicPath )
                => WriteLine( $"A message handler for {topicPath} was closed." );

            /// <summary>
            /// Indicates that a message was received.
            /// </summary>
            public void OnMessage( ISessionId sessionId, string topicPath, IContent content, IReceiveContext context )
                => WriteLine( $"Handler for {topicPath} has received message: {content.AsString()}." );
        }
    }
}
