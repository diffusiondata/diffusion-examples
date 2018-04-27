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

using System;
using System.Threading;
using System.Threading.Tasks;
using PushTechnology.ClientInterface.Client.Factories;
using PushTechnology.ClientInterface.Client.Features;
using static System.Console;
using static PushTechnology.ClientInterface.Examples.Runner.Program;

namespace PushTechnology.ClientInterface.Example.Publishing {
    /// <summary>
    /// Control client implementation that sends messages to a topic path.
    /// </summary>
    public sealed class SendingMessages : IExample {
        /// <summary>
        /// Runs the client sending messages example.
        /// </summary>
        /// <param name="cancellationToken">A token used to end the client example.</param>
        /// <param name="args">A single string should be used for the server url.</param>
        public async Task Run( CancellationToken cancellationToken, string[] args ) {
            var serverUrl = args[ 0 ];
            var session = Diffusion.Sessions.Principal( "control" ).Password( "password" ).Open( serverUrl );
            var messaging = session.Messaging;

            var topicPath = ">random/messages";
            var sendCallback = new SendCallback( topicPath );

            while ( !cancellationToken.IsCancellationRequested ) {
                var newMessage = $"Sending message {DateTime.Now.TimeOfDay.ToString( "g" )} to topic path {topicPath}";
                messaging.Send( topicPath, newMessage, sendCallback );

                await Task.Delay( 500 );
            }

            // Close the session
            session.Close();
        }

        /// <summary>
        /// A simple ISendCallback implementation that prints confirmation of the actions completed.
        /// </summary>
        private sealed class SendCallback : ISendCallback {
            private readonly string topicPath;

            /// <summary>
            /// Constructor.
            /// </summary>
            /// <param name="topicPath">The topic path.</param>
            public SendCallback( string topicPath ) => this.topicPath = topicPath;

            /// <summary>
            /// Indicates a successful sending of a message.
            /// </summary>
            public void OnComplete()
                => WriteLine( $"A message was sent successfully to {topicPath}." );

            /// <summary>
            /// Indicates that a message has been discarded.
            /// </summary>
            public void OnDiscard()
                => WriteLine( $"A message to {topicPath} has been discarded." );
        }
    }
}
