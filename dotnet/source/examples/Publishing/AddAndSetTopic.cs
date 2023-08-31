/**
 * Copyright © 2020 - 2023 DiffusionData Ltd.
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
using PushTechnology.ClientInterface.Client.Session;
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
            string serverUrl = args[ 0 ];
            var session = Diffusion.Sessions.Principal( "control" ).Password( "password" )
                .CertificateValidation((cert, chain, errors) => CertificateValidationResult.ACCEPT)
                .Open(serverUrl);

            var topicUpdate = session.TopicUpdate;

            try {
                // Attempt to add a topic, set its value to 0, and await the response from the server.
                // If there was no topic previously bound to "test/topic", the method will return and create the topic and set its value to 0.
                // If there is an existing INT64 topic with default topic properties bound to "test/topic" the method will set its value to 1.
                // If an incompatible topic is bound to "test/topic", the method will throw ExistingTopicException.
                var state = await topicUpdate.AddAndSetAsync<long?>( "test/topic", session.TopicControl.NewSpecification( TopicType.INT64 ), 0L );

                WriteLine($"Topic 'test/topic' {state} with value: 0." );

                state = await topicUpdate.AddAndSetAsync<long?>( "test/topic", session.TopicControl.NewSpecification( TopicType.INT64 ), 1L );

                WriteLine($"Topic 'test/topic' {state} with value: 1.");
            }
            catch ( Exception ex ) {
                WriteLine( $"Failed to add and set topic 'test/topic': {ex}." );
                session.Close();

                return;
            }

            try
            {
                await session.TopicControl.RemoveTopicsAsync("test/topic");

                WriteLine($"Removed topic 'test/topic'.");
            }
            catch (Exception ex)
            {
                WriteLine($"Failed to remove topic 'test/topic': {ex}.");
            }
            finally
            {
                session.Close();
            }
        }
    }
}


