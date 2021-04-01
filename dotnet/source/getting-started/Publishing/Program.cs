﻿/**
 * Copyright © 2016, 2017 Push Technology Ltd.
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
using PushTechnology.ClientInterface.Client.Callbacks;
using PushTechnology.ClientInterface.Client.Factories;
using PushTechnology.ClientInterface.Client.Features.Control.Topics;
using PushTechnology.ClientInterface.Client.Topics;
using PushTechnology.ClientInterface.Data.JSON;

namespace PushTechnology.ClientInterface.Example {
    /// <summary>
    /// A client that publishes an incrementing count to the JSON topic "foo/counter".
    /// </summary>
    class Program {
        static void Main( string[] args ) {
            // Connect using a principal with 'modify_topic' and 'update_topic' permissions
            var session = Diffusion.Sessions.Principal( "control" ).Password( "password" ).Open( "ws://localhost:8080" );

            // Get the TopicControl and TopicUpdateControl features
            var topicControl = session.TopicControl;
            var updateControl = session.TopicUpdateControl;

            // Create a JSON topic 'foo/counter'
            var topic = "foo/counter";
            try {
                topicControl.AddTopicAsync( topic, TopicType.JSON ).Wait();
            } catch ( Exception ex ) {
                Console.WriteLine( $"Failed to add topic {topic} : {ex}." );
                session.Close();
                return;
            }

            // Update topic every 300 ms for 30 minutes
            var updateCallback = new UpdateCallback( topic );
            for ( var i = 0; i < 3600; ++i ) {
                var newValue = Diffusion.DataTypes.JSON.FromJSONString(
                    "{\"date\":\"" + DateTime.Today.Date.ToString( "D" ) + "\"," +
                    "\"time\":\"" + DateTime.Now.TimeOfDay.ToString( "g" ) + "\"}" );
                updateControl.Updater.ValueUpdater<IJSON>().Update( topic, newValue, updateCallback );

                Thread.Sleep( 300 );
            }

            // Close session
            session.Close();
        }

        /// <summary>
        /// A simple ITopicUpdaterUpdateCallback implementation that prints confimation of the actions completed.
        /// </summary>
        class UpdateCallback : ITopicUpdaterUpdateCallback {
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
                => Console.WriteLine( $"Topic {topicPath} could not be updated : {errorReason}" );

            /// <summary>
            /// Indicates a successful update.
            /// </summary>
            public void OnSuccess() 
                => Console.WriteLine( $"Topic {topicPath} updated successfully." );
        }
    }
}
