/**
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
using PushTechnology.ClientInterface.Data.JSON;

namespace PushTechnology.ClientInterface.GettingStarted {
    /// <summary>
    /// A client that publishes an incrementing count to the JSON topic "foo/counter".
    /// </summary>
    public sealed class PublishingClient {
        public static void Main( string[] args ) {
            // Connect using a principal with 'modify_topic' and 'update_topic' permissions
            var session = Diffusion.Sessions.Principal( "control" ).Password( "password" ).Open( "ws://localhost:8080" );

            // Get the TopicControl and TopicUpdateControl features
            var topicControl = session.TopicControl;
            var updateControl = session.TopicUpdateControl;

            // Create a JSON topic 'foo/counter'
            var topic = "foo/counter";
            var addCallback = new AddCallback();
            topicControl.AddTopicFromValue(
                topic,
                Diffusion.DataTypes.JSON.FromJSONString( "{\"date\":\"To be updated\",\"time\":\"To be updated\"}" ),
                addCallback );

            // Wait for the OnTopicAdded callback, or a failure
            if ( !addCallback.Wait( TimeSpan.FromSeconds( 5 ) ) ) {
                Console.WriteLine( "Callback not received within timeout." );
                session.Close();
                return;
            } else if ( addCallback.Error != null ) {
                Console.WriteLine( "Error : {0}", addCallback.Error.ToString() );
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
    }

    /// <summary>
    /// Basic implementation of the ITopicControlAddCallback.
    /// </summary>
    internal sealed class AddCallback : ITopicControlAddCallback {
        private readonly AutoResetEvent resetEvent = new AutoResetEvent( false );

        /// <summary>
        /// Any error from this AddCallback will be stored here.
        /// </summary>
        public Exception Error {
            get;
            private set;
        }

        /// <summary>
        /// Constructor.
        /// </summary>
        public AddCallback() {
            Error = null;
        }

        /// <summary>
        /// This is called to notify that a call context was closed prematurely, typically due to a timeout or the
        /// session being closed.
        /// </summary>
        /// <remarks>
        /// No further calls will be made for the context.
        /// </remarks>
        public void OnDiscard() {
            Error = new Exception( "This context was closed prematurely." );
            resetEvent.Set();
        }

        /// <summary>
        /// This is called to notify that the topic has been added.
        /// </summary>
        /// <param name="topicPath">The full path of the topic that was added.</param>
        public void OnTopicAdded( string topicPath ) {
            Console.WriteLine( "Topic {0} added.", topicPath );
            resetEvent.Set();
        }

        /// <summary>
        /// This is called to notify that an attempt to add a topic has failed.
        /// </summary>
        /// <param name="topicPath">The topic path as supplied to the add request.</param>
        /// <param name="reason">The reason for failure.</param>
        public void OnTopicAddFailed( string topicPath, TopicAddFailReason reason ) {
            Error = new Exception( string.Format( "Failed to add topic {0} : {1}", topicPath, reason ) );
            resetEvent.Set();
        }

        /// <summary>
        /// Wait for one of the callbacks for a given time.
        /// </summary>
        /// <param name="timeout">Time to wait for the callback.</param>
        /// <returns><c>true</c> if either of the callbacks has been triggered. Otherwise <c>false</c>.</returns>
        public bool Wait( TimeSpan timeout ) {
            return resetEvent.WaitOne( timeout );
        }
    }

    /// <summary>
    /// A simple ITopicUpdaterUpdateCallback implementation that prints confimation of the actions completed.
    /// </summary>
    internal sealed class UpdateCallback : ITopicUpdaterUpdateCallback {
        private readonly string topicPath;

        /// <summary>
        /// Constructor.
        /// </summary>
        /// <param name="topicPath">The topic path.</param>
        public UpdateCallback( string topicPath ) {
            this.topicPath = topicPath;
        }

        /// <summary>
        /// Notification of a contextual error related to this callback.
        /// </summary>
        /// <remarks>
        /// Situations in which <code>OnError</code> is called include the session being closed, a communication
        /// timeout, or a problem with the provided parameters. No further calls will be made to this callback.
        /// </remarks>
        /// <param name="errorReason">A value representing the error.</param>
        public void OnError( ErrorReason errorReason ) {
            Console.WriteLine( "Topic {0} could not be updated : {1}", topicPath, errorReason );
        }

        /// <summary>
        /// Indicates a successful update.
        /// </summary>
        public void OnSuccess() {
            Console.WriteLine( "Topic {0} updated successfully.", topicPath );
        }
    }
}
