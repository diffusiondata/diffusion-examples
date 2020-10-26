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
    /// Control client implementation that appends values with a user supplied timestamp to a time series topic.
    /// </summary>
    public sealed class PublishingTimeSeriesAppendWithTimestamp : IExample {
        /// <summary>
        /// Runs the time series topic append (with timestamp) control client example.
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
                await session.TimeSeries.AppendAsync<String>(topicPath, "Value1", DateTimeOffset.FromUnixTimeMilliseconds(322), cancellationToken);
                await session.TimeSeries.AppendAsync<String>(topicPath, "Value1", DateTimeOffset.FromUnixTimeMilliseconds(323), cancellationToken);
                await session.TimeSeries.AppendAsync<String>(topicPath, "Value1", DateTimeOffset.FromUnixTimeMilliseconds(323), cancellationToken);
                await session.TimeSeries.AppendAsync<String>(topicPath, "Value1", DateTimeOffset.FromUnixTimeMilliseconds(324), cancellationToken);
                await session.TimeSeries.AppendAsync<String>(topicPath, "Value1", DateTimeOffset.FromUnixTimeMilliseconds(325), cancellationToken);

                await Task.Delay(TimeSpan.FromMilliseconds(300));
            }
            catch (Exception ex)
            {
                WriteLine($"Topic {topicPath} value could not be appended : {ex}.");
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
