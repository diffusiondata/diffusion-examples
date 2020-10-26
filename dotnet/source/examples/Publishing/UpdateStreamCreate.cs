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
using PushTechnology.ClientInterface.Client.Callbacks;
using PushTechnology.ClientInterface.Client.Factories;
using PushTechnology.ClientInterface.Client.Features.Control.Topics;
using PushTechnology.ClientInterface.Client.Session;
using PushTechnology.ClientInterface.Client.Topics;
using PushTechnology.ClientInterface.Client.Topics.Details;
using static System.Console;
using static PushTechnology.ClientInterface.Examples.Runner.Program;

namespace PushTechnology.ClientInterface.Example.Publishing {
    /// <summary>
    /// Control client implementation that creates an update stream.
    /// </summary>
    public sealed class UpdateStreamCreate : IExample {
        /// <summary>
        /// Runs the create update stream control client example.
        /// </summary>
        /// <param name="cancellationToken">A token used to end the client example.</param>
        /// <param name="args">A single string should be used for the server url.</param>
        public async Task Run( CancellationToken cancellationToken, string[] args ) {
            string TOPIC_PREFIX = "topic-updates";

            var serverUrl = args[ 0 ];
            var session = Diffusion.Sessions.Principal( "control" ).Password( "password" )
                .CertificateValidation((cert, chain, errors) => CertificateValidationResult.ACCEPT)
                .Open( serverUrl );

            var typeName = Diffusion.DataTypes.Get<String>().TypeName;
            var topicPath = $"{TOPIC_PREFIX}/{typeName}/{DateTime.Now.ToFileTimeUtc()}";
            var specification = session.TopicControl.NewSpecification(TopicType.STRING);
            var stream = session.TopicUpdate.CreateUpdateStream<String>(topicPath, specification);

            try
            {
                await stream.SetAsync("Value1", cancellationToken);
                WriteLine($"Topic '{topicPath}' added successfully with value set to '{stream.Value}'.");

                await stream.SetAsync("Value2", cancellationToken);
                WriteLine($"Topic '{topicPath}' value successfully set to '{stream.Value}'.");
            }
            catch (Exception ex)
            {
                WriteLine($"Failed to add topic '{topicPath}' and set value : {ex}.");
                session.Close();
                return;
            }

            // Close the session
            session.Close();
        }
    }
}
