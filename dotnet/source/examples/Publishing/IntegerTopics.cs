﻿/**
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
using PushTechnology.ClientInterface.Client.Callbacks;
using PushTechnology.ClientInterface.Client.Factories;
using PushTechnology.ClientInterface.Client.Features.Control.Topics;
using PushTechnology.ClientInterface.Client.Topics;
using static System.Console;
using static PushTechnology.ClientInterface.Examples.Runner.Program;

namespace PushTechnology.ClientInterface.Example.Publishing {
    /// <summary>
    /// Control client implementation that adds and updates an integer topic.
    /// </summary>
    public sealed class PublishingIntegerTopics : IExample {
        /// <summary>
        /// Runs the integer topic control client example.
        /// </summary>
        /// <param name="cancellationToken">A token used to end the client example.</param>
        /// <param name="args">A single string should be used for the server url.</param>
        public async Task Run( CancellationToken cancellationToken, string[] args ) {
            var serverUrl = args[ 0 ];
            var session = Diffusion.Sessions.Principal( "control" ).Password( "password" ).Open( serverUrl );
            var topicControl = session.TopicControl;
            var updateControl = session.TopicUpdateControl;

            var random = new Random();

            // Create a Integer topic 'random/Integer'
            var topic = "random/Integer";

            try {
                await topicControl.AddTopicAsync( topic, TopicType.INT64, cancellationToken );
            } catch ( Exception ex ) {
                WriteLine( $"Failed to add topic '{topic}' : {ex}." );
                session.Close();
                return;
            }

            // Update topic every 300 ms until user requests cancellation of example
            var updateCallback = new UpdateCallback( topic );
            var valueUpdater = updateControl.Updater.ValueUpdater<long?>();

            while ( !cancellationToken.IsCancellationRequested ) {
                var newValue = (long) random.Next();
                valueUpdater.Update( topic, newValue, updateCallback );

                await Task.Delay( 300 );
            }

            // Remove the Integer topic 'random/Integer'
            try {
                await topicControl.RemoveTopicsAsync( topic, cancellationToken );
            } catch ( Exception ex ) {
                WriteLine( $"Failed to remove topic '{topic}' : {ex}." );
            }

            // Close the session
            session.Close();
        }

        /// <summary>
        /// A simple ITopicUpdaterUpdateCallback implementation that prints confimation of the actions completed.
        /// </summary>
        private sealed class UpdateCallback : ITopicUpdaterUpdateCallback {
            private readonly string topicPath;

            /// <summary>
            /// Constructor.
            /// </summary>
            /// <param name="topicPath">The topic path.</param>
            public UpdateCallback( string topicPath )
                => this.topicPath = topicPath;

            /// <summary>
            /// Notification of a contextual error related to this callback.
            /// </summary>
            /// <remarks>
            /// Situations in which <code>OnError</code> is called include the session being closed, a communication
            /// timeout, or a problem with the provided parameters. No further calls will be made to this callback.
            /// </remarks>
            /// <param name="errorReason">A value representing the error.</param>
            public void OnError( ErrorReason errorReason )
                => WriteLine( $"Topic {topicPath} could not be updated : {errorReason}." );

            /// <summary>
            /// Indicates a successful update.
            /// </summary>
            public void OnSuccess()
                => WriteLine( $"Topic {topicPath} updated successfully." );
        }
    }
}
