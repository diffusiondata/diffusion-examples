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
using System.Collections.Generic;
using System.Threading;
using System.Threading.Tasks;
using PushTechnology.ClientInterface.Client.Factories;
using PushTechnology.ClientInterface.Client.Session;
using PushTechnology.ClientInterface.Client.Topics;
using static System.Console;
using static PushTechnology.ClientInterface.Examples.Runner.Program;

namespace PushTechnology.ClientInterface.Example.Publishing {
    /// <summary>
    /// Control client implementation that adds and updates a RecordV2 topic.
    /// </summary>
    public sealed class PublishingRecordV2Topics : IExample {
        /// <summary>
        /// Runs the RecordV2 topic control client example.
        /// </summary>
        /// <param name="cancellationToken">A token used to end the client example.</param>
        /// <param name="args">A single string should be used for the server URL.</param>
        public async Task Run( CancellationToken cancellationToken, string[] args ) {
            string serverUrl = args[ 0 ];
            var session = Diffusion.Sessions.Principal( "control" ).Password( "password" )
                .CertificateValidation((cert, chain, errors) => CertificateValidationResult.ACCEPT)
                .Open(serverUrl);

            var topicControl = session.TopicControl;
            var topicUpdate = session.TopicUpdate;

            var random = new Random();

            // Create a RecordV2 topic 'random/RecordV2'
            string topic = "random/RecordV2";

            try {
                await topicControl.AddTopicAsync( topic, TopicType.RECORD_V2, cancellationToken );

                WriteLine($"Topic '{topic}' added successfully.");
            }
            catch ( Exception ex ) {
                WriteLine( $"Failed to add topic '{topic}' : {ex}." );
                session.Close();
                return;
            }

            WriteLine($"Updating topic '{topic}' with new values:");

            // Update topic every 300 ms until user requests cancellation of example
            while ( !cancellationToken.IsCancellationRequested ) {
                var builder = Diffusion.DataTypes.RecordV2.CreateValueBuilder();
                var fields = new List<string>() { DateTime.Now.ToLongTimeString(), "this", "is", "an", "example" };
                builder.AddFields( fields );
                var newValue = builder.Build();

                try {
                    await topicUpdate.SetAsync( topic, newValue, cancellationToken );

                    await Task.Delay( TimeSpan.FromMilliseconds( 300 ) );
                } catch ( Exception ex ) {
                    WriteLine( $"Topic {topic} could not be updated : {ex}." );
                }
            }

            // Remove the RecordV2 topic 'random/RecordV2'
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
