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
    /// Control client implementation that edits values of a time series topic.
    /// </summary>
    public sealed class PublishingTimeSeriesEdit : IExample {
        /// <summary>
        /// Runs the time series topic edit control client example.
        /// </summary>
        /// <param name="cancellationToken">A token used to end the client example.</param>
        /// <param name="args">A single string should be used for the server url.</param>
        public async Task Run( CancellationToken cancellationToken, string[] args ) {
            string TOPIC_PREFIX = "time-series";

            var serverUrl = args[ 0 ];
            var session = Diffusion.Sessions.Principal( "control" ).Password( "password" )
                .CertificateValidation((cert, chain, errors) => CertificateValidationResult.ACCEPT)
                .Open( serverUrl );

            // Create a string topic
            var typeName = Diffusion.DataTypes.Get<String>().TypeName;
            var topicPath = $"{TOPIC_PREFIX}/{typeName}/{DateTime.Now.ToFileTimeUtc()}";
            var specification = session.TopicControl.NewSpecification(TopicType.TIME_SERIES)
                .WithProperty(TopicSpecificationProperty.TimeSeriesEventValueType, typeName);

            try
            {
                await session.TopicControl.AddTopicAsync(topicPath, specification, cancellationToken);
            }
            catch (Exception ex)
            {
                WriteLine($"Failed to add topic '{topicPath}' : {ex}.");
                session.Close();
                return;
            }

            try
            {
                await session.TimeSeries.AppendAsync<String>(topicPath, "Value 1", cancellationToken);
            }
            catch (Exception ex)
            {
                WriteLine($"Topic {topicPath} value could not be appended : {ex}.");
            }

            try
            {
                await session.TimeSeries.EditAsync<String>(topicPath, 0, "Value 1a", cancellationToken);
                await session.TimeSeries.EditAsync<String>(topicPath, 0, "Value 1b", cancellationToken);
                await session.TimeSeries.EditAsync<String>(topicPath, 0, "Value 1c", cancellationToken);
                await session.TimeSeries.EditAsync<String>(topicPath, 0, "Value 1d", cancellationToken);
            }
            catch (Exception ex)
            {
                WriteLine($"Topic {topicPath} value could not be edited : {ex}.");
            }

            // Remove the string topic
            try
            {
                await session.TopicControl.RemoveTopicsAsync( topicPath, cancellationToken );
            } catch(Exception ex) {
                WriteLine( $"Failed to remove topic '{topicPath}' : {ex}." );
            }

            // Close the session
            session.Close();
        }
    }
}
