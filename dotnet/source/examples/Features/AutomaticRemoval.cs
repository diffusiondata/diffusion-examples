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

using PushTechnology.ClientInterface.Client.Factories;
using PushTechnology.ClientInterface.Client.Topics;
using PushTechnology.ClientInterface.Client.Topics.Details;
using System;
using System.Globalization;
using System.Threading;
using System.Threading.Tasks;
using static PushTechnology.ClientInterface.Examples.Runner.Program;
using static System.Console;

namespace PushTechnology.ClientInterface.Example.Features {
    public class Removal : IExample {
        /// <summary>Client implementation that removes topics based on different topic specifications.</summary>
        /// <param name="cancellationToken">The cancellation token to cancel the current example run.</param>
        /// <param name="args">A single string which should be used for the server url.</param>
        public async Task Run( CancellationToken cancellationToken, string[] args ) {
            var serverUrl = args[ 0 ];
            // Connect with control principal.
            var session = Diffusion.Sessions.Principal( "control" ).Password( "password" ).Open( serverUrl );
            var topicControl = session.TopicControl;
            ITopicSpecification topicSpecification;

            // Topic to be removed after a set time has passed.
            var topicPath = "removal/RemovalOnTime";

            try {
                // Get the time 30 seconds from now as a RFC1123 String.
                var time = DateTime
                    .UtcNow
                    .AddSeconds( 30 )
                    .ToString( DateTimeFormatInfo.InvariantInfo.RFC1123Pattern );

                // Creating a topic specification with the required topic type and property.
                topicSpecification = topicControl
                     .NewSpecification( TopicType.INT64 )
                     .WithProperty( TopicSpecificationProperty.Removal, $"when time after \"{time}\"" );

                // After adding this topic, the removal property will see to its deletion after the set time and date
                // have passed.
                await topicControl.AddTopicAsync( topicPath, topicSpecification, cancellationToken );
                WriteLine( "Topic \"{0}\" with the specification \"{1} : {2}\" has been added.",
                    topicPath,
                    TopicSpecificationProperty.Removal,
                    topicSpecification.Properties[ TopicSpecificationProperty.Removal ] );
            } catch ( Exception ex ) {
                WriteLine( "Failed to add topic {0} : {1}.", topicPath, ex );
            }

            // Topic to be removed after the session that created it is closed.
            topicPath = "removal/RemovalOnSessionClosure";

            try {
                // Creating a topic specification with the required topic type and property.
                topicSpecification = topicControl
                     .NewSpecification( TopicType.INT64 )
                     .WithProperty( TopicSpecificationProperty.Removal, "when this session closes" );

                // After adding this topic, the removal property will see to its deletion after the session closes.
                await topicControl.AddTopicAsync( topicPath, topicSpecification, cancellationToken );
                WriteLine( "Topic \"{0}\" with the specification \"{1} : {2}\" has been added.",
                    topicPath,
                    TopicSpecificationProperty.Removal,
                    topicSpecification.Properties[ TopicSpecificationProperty.Removal ] );
            } catch ( Exception ex ) {
                WriteLine( "Failed to add topic {0} : {1}.", topicPath, ex );
            }

            // Topic to be removed after a set time has passed without any updates.
            topicPath = "removal/RemovalOnNoUpdates";

            try {
                // Creating a topic specification with the required topic type and property.
                topicSpecification = topicControl
                     .NewSpecification( TopicType.INT64 )
                     .WithProperty( TopicSpecificationProperty.Removal, "when no updates for 10s" );

                // After adding this topic, the removal property will see to its deletion when there have been 
                // no updates for 10 seconds.
                await topicControl.AddTopicAsync( topicPath, topicSpecification, cancellationToken );
                WriteLine( "Topic \"{0}\" with the specification \"{1} : {2}\" has been added.",
                    topicPath,
                    TopicSpecificationProperty.Removal,
                    topicSpecification.Properties[ TopicSpecificationProperty.Removal ] );
            } catch ( Exception ex ) {
                WriteLine( "Failed to add topic {0} : {1}.", topicPath, ex );
            }

            // Topic to be removed if specified criteria is met.           
            topicPath = "removal/RemovalOnCriteria";

            try {
                // Creating a topic specification with the required topic type and property.
                topicSpecification = topicControl
                     .NewSpecification( TopicType.INT64 )
                     .WithProperty( TopicSpecificationProperty.Removal,
                     "when no session has \"$Principal is 'yourprincipal'\" after 5s" );

                // After adding this topic, the removal property will see to its deletion when the criteria are met. 
                // In this case, it means that the 'yourprincipal' principal is not authenticated with any session.
                await topicControl.AddTopicAsync(
                    topicPath,
                    topicSpecification,
                    cancellationToken );
                WriteLine( "Topic \"{0}\" with the specification \"{1} : {2}\" has been added.",
                   topicPath,
                   TopicSpecificationProperty.Removal,
                   topicSpecification.Properties[ TopicSpecificationProperty.Removal ] );
            } catch ( Exception ex ) {
                WriteLine( "Failed to add topic {0} : {1}.", topicPath, ex );
            }

            // Topic to be removed when the number of subscriptions falls below a specified value.            
            topicPath = "removal/RemovalOnLessSubscriptions";

            try {
                // Creating a topic specification with the required topic type and property.
                topicSpecification = topicControl
                     .NewSpecification( TopicType.INT64 )
                     .WithProperty( TopicSpecificationProperty.Removal, "when subscriptions < 1 for 10s" );

                // After adding this topic, the removal property will see to its deletion when there are no subscribers.
                await topicControl.AddTopicAsync( topicPath, topicSpecification, cancellationToken );
                WriteLine( "Topic \"{0}\" with the specification \"{1} : {2}\" has been added.",
                    topicPath,
                    TopicSpecificationProperty.Removal,
                    topicSpecification.Properties[ TopicSpecificationProperty.Removal ] );
            } catch ( Exception ex ) {
                WriteLine( "Failed to add topic {0} : {1}.", topicPath, ex );
            }

            session.Close();
        }
    }
}
