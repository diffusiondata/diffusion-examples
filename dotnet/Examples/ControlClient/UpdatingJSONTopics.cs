/**
 * Copyright © 2016 Push Technology Ltd.
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
using PushTechnology.ClientInterface.Examples.Runner;

namespace PushTechnology.ClientInterface.Examples.Control {

    /// <summary>
    /// Control Client implementation that Adds and Updates a JSON topic.
    /// </summary>
    public sealed class UpdatingJSONTopics : IExample {
        /// <summary>
        /// Runs the JSON topic Control Client example.
        /// </summary>
        /// <param name="cancellationToken">A token used to end the client example.</param>
        /// <param name="args">A single string should be used for the server url.</param>
        public void Run( CancellationToken cancellationToken, string[] args ) {
            var serverUrl = args[ 0 ];
            var session = Diffusion.Sessions.Principal( "control" ).Password( "password" ).Open( serverUrl );
            var topicControl = session.GetTopicControlFeature();
            var updateControl = session.GetTopicUpdateControlFeature();

            // Create a JSON topic 'random/JSON'
            var topic = "random/JSON";
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

            // Update topic every 300 ms until user requests cancellation of enxample
            var updateCallback = new UpdateCallback( topic );
            while ( !cancellationToken.IsCancellationRequested ) {
                var newValue = Diffusion.DataTypes.JSON.FromJSONString(
                    "{\"date\":\"" + DateTime.Today.Date.ToString( "D" ) + "\"," +
                    "\"time\":\"" + DateTime.Now.TimeOfDay.ToString( "g" ) + "\"}" );
                updateControl.Updater.ValueUpdater<IJSON>().Update( topic, newValue, updateCallback );

                Thread.Sleep( 300 );
            }

            // Remove the JSON topic 'random/JSON'
            var removeCallback = new RemoveCallback( topic );
            topicControl.Remove( topic, removeCallback );
            if ( !removeCallback.Wait( TimeSpan.FromSeconds( 5 ) ) ) {
                Console.WriteLine( "Callback not received within timeout." );
            } else if ( removeCallback.Error != null ) {
                Console.WriteLine( "Error : {0}", removeCallback.Error.ToString() );
            }

            // Close the session
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

    /// <summary>
    /// Basic implementation of the ITopicRemovalCallback.
    /// </summary>
    internal sealed class RemoveCallback : ITopicControlRemovalCallback {
        private readonly AutoResetEvent resetEvent = new AutoResetEvent( false );
        private readonly string topicPath;

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
        /// <param name="topicPath">The topic path.</param>
        public RemoveCallback( string topicPath ) {
            this.topicPath = topicPath;
            Error = null;
        }

        /// <summary>
        /// Notification that a call context was closed prematurely, typically due to a timeout or
        /// the session being closed.
        /// </summary>
        /// <param name="errorReason">The error reason.</param>
        /// <remarks>
        /// No further calls will be made for the context.
        /// </remarks>
        public void OnError( ErrorReason errorReason ) {
            Error = new Exception( "This context was closed prematurely. Reason=" + errorReason.ToString() );
            resetEvent.Set();
        }

        /// <summary>
        /// Topic(s) have been removed.
        /// </summary>
        public void OnTopicsRemoved() {
            Console.WriteLine( "Topic {0} removed", topicPath );
            resetEvent.Set();
        }

        /// <summary>
        /// Wait for one of the callbacks for a given time.
        /// </summary>
        /// <param name="timeout">Time to wait for the callback.</param>
        /// <returns><c>true</c> if either of the callbacks has been triggered, and <c>false</c> otherwise.</returns>
        public bool Wait( TimeSpan timeout ) {
            return resetEvent.WaitOne( timeout );
        }
    }
}
