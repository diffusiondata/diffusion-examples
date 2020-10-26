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
using PushTechnology.ClientInterface.Client.Session;
using PushTechnology.ClientInterface.Client.Topics;
using static System.Console;
using static PushTechnology.ClientInterface.Examples.Runner.Program;

namespace PushTechnology.ClientInterface.Example.Publishing {
    /// <summary>
    /// Control client implementation that adds then removes a string topic.
    /// </summary>
    public sealed class RemoveTopics : IExample {
        /// <summary>
        /// Runs the remove topic control client example.
        /// </summary>
        /// <param name="cancellationToken">A token used to end the client example.</param>
        /// <param name="args">A single string should be used for the server url.</param>
        public async Task Run( CancellationToken cancellationToken, string[] args ) {
            string TOPIC_PREFIX = "test-topics";

            var serverUrl = args[ 0 ];
            var session = Diffusion.Sessions.Principal("control").Password("password")
                .CertificateValidation((cert, chain, errors) => CertificateValidationResult.ACCEPT)
                .Open(serverUrl);
            var topicControl = session.TopicControl;

            // Create a string topic
            var topicPath = $"{TOPIC_PREFIX}/String";

            // Add topic
            try
            {
                await topicControl.AddTopicAsync(topicPath, TopicType.STRING, cancellationToken );

                WriteLine($"Topic '{topicPath}' successfully added.");
            }
            catch (Exception ex) {
                WriteLine( $"Failed to add topic '{topicPath}' : {ex}." );
                session.Close();
                return;
            }

            // Add again but this will fail as the topic already exists
            try
            {
                var addTopicResult = await topicControl.AddTopicAsync(topicPath, TopicType.STRING, cancellationToken);

                var result = (addTopicResult == Client.Features.Control.Topics.AddTopicResult.EXISTS) ? "already exists so could not be added": "does not exist so was added";
                WriteLine($"Topic '{topicPath}' {result}.");
            }
            catch (Exception ex)
            {
                WriteLine($"Failed to add topic '{topicPath}' : {ex}.");
                session.Close();
                return;
            }

            // Remove the string topic
            try
            {
                await topicControl.RemoveTopicsAsync($"?{TOPIC_PREFIX}/", cancellationToken );

                WriteLine($"Topic '{topicPath}' successfully removed.");
            }
            catch (Exception ex) {
                WriteLine( $"Failed to remove topic '{topicPath}' : {ex}." );
            }

            // Add again but this time there is success
            try
            {
                var addTopicResult = await topicControl.AddTopicAsync(topicPath, TopicType.STRING, cancellationToken);

                var result = (addTopicResult == Client.Features.Control.Topics.AddTopicResult.EXISTS) ? "already exists so could not be added" : "does not exist so was added";
                WriteLine($"Topic '{topicPath}' {result}.");
            }
            catch (Exception ex)
            {
                WriteLine($"Failed to add topic '{topicPath}' : {ex}.");
                session.Close();
                return;
            }

            // Close the session
            session.Close();
        }
    }
}
