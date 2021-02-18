/**
 * Copyright © 2018, 2019 Push Technology Ltd.
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
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using PushTechnology.ClientInterface.Client.Callbacks;
using PushTechnology.ClientInterface.Client.Factories;
using PushTechnology.ClientInterface.Client.Features.Control.Topics;
using PushTechnology.ClientInterface.Client.Topics;
using PushTechnology.ClientInterface.Data.Binary;
using static System.Console;
using static PushTechnology.ClientInterface.Examples.Runner.Program;

namespace PushTechnology.ClientInterface.Example.Publishing {
    /// <summary>
    /// Control client implementation that adds and updates a binary topic.
    /// </summary>
    public sealed class PublishingBinaryTopics : IExample {
        /// <summary>
        /// Runs the binary topic control client example.
        /// </summary>
        /// <param name="cancellationToken">A token used to end the client example.</param>
        /// <param name="args">A single string should be used for the server url.</param>
        public async Task Run( CancellationToken cancellationToken, string[] args ) {
            var serverUrl = args[ 0 ];
            var session = Diffusion.Sessions.Principal( "control" ).Password( "password" ).Open( serverUrl );
            var topicControl = session.TopicControl;
            var topicUpdate = session.TopicUpdate;

            var random = new Random();

            // Create a binary topic 'random/Binary'
            var topic = "random/Binary";

            try {
                await topicControl.AddTopicAsync( topic, TopicType.BINARY, cancellationToken );
            } catch ( Exception ex ) {
                WriteLine( $"Failed to add topic '{topic}' : {ex}." );
                session.Close();
                return;
            }

            // Update topic every 300 ms until user requests cancellation of example
            while ( !cancellationToken.IsCancellationRequested ) {
                var newValue = Diffusion.DataTypes.Binary.ReadValue(
                    Encoding.UTF8.GetBytes( DateTime.Now.ToLongTimeString() ) );

                try {
                    await topicUpdate.SetAsync( topic, newValue, cancellationToken );
                    Console.WriteLine( $"Topic {topic} updated successfully." );

                    await Task.Delay( TimeSpan.FromMilliseconds( 300 ) );
                } catch ( Exception ex ) {
                    Console.WriteLine( $"Topic {topic} could not be updated : {ex}." );
                }
            }

            // Remove the binary topic 'random/Binary'
            try {
                await topicControl.RemoveTopicsAsync( topic, cancellationToken );
            } catch ( Exception ex ) {
                WriteLine( $"Failed to remove topic '{topic}' : {ex}." );
            }

            // Close the session
            session.Close();
        }
    }
}
