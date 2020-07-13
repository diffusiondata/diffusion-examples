/**
 * Copyright © 2020 Push Technology Ltd.
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
using PushTechnology.ClientInterface.Client.Topics;
using static System.Console;
using static PushTechnology.ClientInterface.Examples.Runner.Program;


namespace PushTechnology.ClientInterface.Example.Publishing {
    /// <summary>
    /// Control client implementation that adds a topic if it doesn't exist, or updates the same topic if it does exist.
    /// </summary>
    public sealed class AddAndSetTopic : IExample {
        /// <summary>
        /// Runs the AddAndSet topic update client example.
        /// </summary>
        /// <param name="cancellationToken">A token used to end the client example.</param>
        /// <param name="args">A single string should be used for the server url.</param>
        public async Task Run( CancellationToken cancel, string[] args ) {
            var serverUrl = args[ 0 ];
            var session = Diffusion.Sessions.Principal( "control" ).Password( "password" ).Open( serverUrl );
            var topicUpdate = session.TopicUpdate;

            try {
                // Attempt to add a topic, set its value to 0, and await the response from the server.
                // If there was no topic previously bound to "test/topic", the method will return and create the topic, set the value, and return false.
                // If there is an existing INT64 topic with default topic properties bound to "test/topic" the method will set its value to 0.
                // If an incompatible topic is bound to "test/topic", the method will throw ExistingTopicException.
                var doesExist = await topicUpdate.AddAndSetAsync( "test/topic", session.TopicControl.NewSpecification( TopicType.INT64 ), 0 );

                WriteLine( "Topic test/topic exists:" + doesExist + "with value: " + 0 );

                doesExist = await topicUpdate.AddAndSetAsync( "test/topic", session.TopicControl.NewSpecification( TopicType.INT64 ), 1 );

                WriteLine( "Topic test/topic exists:" + doesExist + "with value: " + 1 );

            } catch ( Exception ex ) {
                WriteLine( $"Failed to add topic : {ex}." );
                return;
            } finally {
                session.Close();
            }
        }
    }
}


