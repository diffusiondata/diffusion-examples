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
using System.Threading.Tasks;
using PushTechnology.ClientInterface.Client.Callbacks;
using PushTechnology.ClientInterface.Client.Content;
using PushTechnology.ClientInterface.Client.Factories;
using PushTechnology.ClientInterface.Client.Features.Control.Topics;
using PushTechnology.ClientInterface.Examples.Runner;

using static System.Console;

namespace PushTechnology.ClientInterface.Examples.Client {
    /// <summary>
    /// Control Client implementation that Adds and Updates a Record topic.
    /// </summary>
    public sealed class UpdatingRecordTopics : IExample {
        /// <summary>
        /// Returns a string array of random values.
        /// </summary>
        /// <remarks>
        /// The array length will always be 10.
        /// </remarks>
        /// <returns>The string array of random values.</returns>
        private static string[] GenerateRandomValues() {
            var values = new string[10];
            var randomInt = new Random();

            for ( int i = 0; i < values.Length; i++ ) {
                values[ i ] = randomInt.Next( 10 ).ToString();
            }

            return values;
        }

        /// <summary>
        /// Runs the Control Client Record topics example.
        /// </summary>
        /// <param name="cancellationToken">A token used to end the client example.</param>
        /// <param name="args">A single string should be used for the server url.</param>
        public async Task Run( CancellationToken cancellationToken, string[] args ) {
            var serverUrl = args[ 0 ];
            var session = Diffusion.Sessions.Principal( "control" ).Password( "password" ).Open( serverUrl );
            var topicControl = session.TopicControl;
            var updateControl = session.TopicUpdateControl;

            // Create a Record topic 'random/record'
            var topic = "random/record";
            var addCallback = new AddCallback();

            // Create Record Content
            var recordBuilder = Diffusion.Content.NewRecordBuilder();

            recordBuilder.Add( GenerateRandomValues() );

            var recordContentBuilder = Diffusion.Content.NewBuilder<IRecordContentBuilder>();

            recordContentBuilder.PutRecords( recordBuilder.Build() );

            var returnContent = recordContentBuilder.Build();

            // Because the topic is created from value, the metadata shouldn't be defined
            // and it will be induced from the data contained in the fields.
            topicControl.AddTopicFromValue( topic, returnContent, addCallback );

            // Wait for the OnTopicAdded callback, or a failure
            if ( !addCallback.Wait( TimeSpan.FromSeconds( 5 ) ) ) {
                WriteLine( "Callback not received within timeout." );
                session.Close();
                return;
            }
            if ( addCallback.Error != null ) {
                WriteLine( $"Error : {addCallback.Error}" );
                session.Close();
                return;
            }

            // Update topic every second.
            var updateCallback = new UpdateCallback( topic );

            while ( !cancellationToken.IsCancellationRequested ) {
                recordContentBuilder.Reset();
                recordBuilder.Reset();

                var newValue = recordContentBuilder.
                    PutRecords( recordBuilder.Add( GenerateRandomValues() ).Build() ).
                    Build();

                updateControl.Updater.Update( topic, newValue, updateCallback );

                Thread.Sleep( 1000 );
            }

            // Remove the Record topic 'random/record'
            var removeCallback = new RemoveCallback( topic );

            topicControl.Remove( topic, removeCallback );

            if ( !removeCallback.Wait( TimeSpan.FromSeconds( 5 ) ) ) {
                WriteLine( "Callback not received within timeout." );
                session.Close();
            } else if ( removeCallback.Error != null ) {
                WriteLine( $"Error : {addCallback.Error}" );
                session.Close();
            }

            // Close session
            session.Close();
        }

        /// <summary>
        /// Basic implementation of the ITopicControlAddCallback.
        /// </summary>
        private sealed class AddCallback : ITopicControlAddCallback {
            private readonly AutoResetEvent resetEvent = new AutoResetEvent( false );

            /// <summary>
            /// Any error from this AddCallback will be stored here.
            /// </summary>
            public Exception Error { get; private set; }

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
                WriteLine( $"Topic {topicPath} added." );
                resetEvent.Set();
            }

            /// <summary>
            /// This is called to notify that an attempt to add a topic has failed.
            /// </summary>
            /// <param name="topicPath">The topic path as supplied to the add request.</param>
            /// <param name="reason">The reason for failure.</param>
            public void OnTopicAddFailed( string topicPath, TopicAddFailReason reason ) {
                Error = new Exception( $"Failed to add topic {topicPath} : '{reason}" );
                resetEvent.Set();
            }

            /// <summary>
            /// Wait for one of the callbacks for a given time.
            /// </summary>
            /// <param name="timeout">Time to wait for the callback.</param>
            /// <returns><c>true</c> if either of the callbacks has been triggered, and <c>false</c> otherwise.</returns>
            public bool Wait( TimeSpan timeout ) => resetEvent.WaitOne( timeout );
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
            public void OnError( ErrorReason errorReason ) => WriteLine( $"Topic {topicPath} could not be updated : {errorReason}" );

            /// <summary>
            /// Indicates a successful update.
            /// </summary>
            public void OnSuccess() => WriteLine( $"Topic {topicPath} updated successfully." );
        }

        /// <summary>
        /// Basic implementation of the ITopicRemovalCallback.
        /// </summary>
        private sealed class RemoveCallback : ITopicControlRemovalCallback {
            private readonly AutoResetEvent resetEvent = new AutoResetEvent( false );
            private readonly string topicPath;

            /// <summary>
            /// Any error from this AddCallback will be stored here.
            /// </summary>
            public Exception Error { get; private set; }

            /// <summary>
            /// Constructor.
            /// </summary>
            /// <param name="topicPath">The topic path.</param>
            public RemoveCallback( string topicPath ) {
                this.topicPath = topicPath;
            }

            /// <summary>
            /// Notification that a call context was closed prematurely, typically due to a timeout
            /// or the session being closed.
            /// </summary>
            /// <remarks>
            /// No further calls will be made for the context.
            /// </remarks>
            public void OnError( ErrorReason errorReason ) {
                Error = new Exception( "This context was closed prematurely. Reason=" + errorReason );
                resetEvent.Set();
            }

            /// <summary>
            /// Topic(s) have been removed.
            /// </summary>
            public void OnTopicsRemoved() {
                WriteLine( $"Topic {topicPath} removed" );
                resetEvent.Set();
            }

            /// <summary>
            /// Wait for one of the callbacks for a given time.
            /// </summary>
            /// <param name="timeout">Time to wait for the callback.</param>
            /// <returns><c>true</c> if either of the callbacks has been triggered, and <c>false</c> otherwise.</returns>
            public bool Wait( TimeSpan timeout ) => resetEvent.WaitOne( timeout );
        }

    }
}
