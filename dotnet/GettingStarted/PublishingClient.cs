/**
 * Copyright © 2014, 2015 Push Technology Ltd.
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

namespace UCIStack.GettingStarted
{
    /// <summary>
    /// A client that publishes an incrementing count to the topic "foo/counter".
    /// </summary>
    class PublishingClient
    {
        static void Main( string[] args )
        {
            StartPublishing();
        }

        #region Public Methods

        private static async void StartPublishing()
        {
            // Connect using a principal with 'modify_topic' and 'update_topic' permissions
            var session = Diffusion.Sessions.Principal( "principal" ).Password( "password" ).Open( "ws://host:80" );

            // Get the TopicControl and TopicUpdateControl features
            var topicControl = session.GetTopicControlFeature();

            var updateControl = session.GetTopicUpdateControlFeature();

            var tcs = new TaskCompletionSource<string>();

            // Create a single-value topic 'foo/counter'
            topicControl.AddTopic( "foo/counter", TopicType.SINGLE_VALUE, new AddCallback( tcs ) );

            // Wait for the OnTopicAdded callback, or a failure
            try
            {
                await tcs.Task;
            }
            catch( Exception )
            {
                // Handle the failure
            }

            // Update the topic
            for( var i = 0; i < 1000; ++i )
            {
                // Use the non-exclusive updater to update the topic without locking it
                updateControl.Updater.Update( "foo/counter", Convert.ToString( i ), new UpdateCallback() );

                Thread.Sleep( 1000 );
            }
        }

        #endregion Public Methods

        #region Private Classes

        private class AddCallback : ITopicControlAddCallback
        {
            #region Fields

            private readonly TaskCompletionSource<string> theCompletionSource;

            #endregion Fields

            #region Constructor

            public AddCallback( TaskCompletionSource<string> completionSource )
            {
                theCompletionSource = completionSource;
            }

            #endregion Constructor

            /// <summary>
            /// This is called to notify that a call context was closed prematurely, typically due to a timeout or the 
            /// session being closed.  No further calls will be made for the context.
            /// </summary>
            public void OnDiscard()
            {
                theCompletionSource.SetException( new Exception( "This context was closed prematurely." ) );
            }

            /// <summary>
            /// Topic has been added.
            /// </summary>
            /// <param name="topicPath">The full path of the topic that was added.</param>
            public void OnTopicAdded( string topicPath )
            {
                theCompletionSource.SetResult( topicPath );
            }

            /// <summary>
            /// An attempt to add a topic has failed.
            /// </summary>
            /// <param name="topicPath">The topic path as supplied to the add request.</param><param name="reason">The reason for failure.</param>
            public void OnTopicAddFailed( string topicPath, TopicAddFailReason reason )
            {
                theCompletionSource.SetException( 
                    new Exception( string.Format( "Failed to add topic {0} because of '{1}", topicPath, reason )) );
            }
        }

        private class UpdateCallback : ITopicUpdaterUpdateCallback
        {
            /// <summary>
            /// Notification of a contextual error related to this callback. This is
            /// analogous to an exception being raised. Situations in which
            /// <code>
            /// OnError
            /// </code>
            ///  is called include the session being closed, a
            ///  communication timeout, or a problem with the provided parameters. No
            ///  further calls will be made to this callback.
            /// </summary>
            /// <param name="errorReason">errorReason a value representing the error; this can be one of
            ///  constants defined in <see cref="T:PushTechnology.ClientInterface.Client.Callbacks.ErrorReason"/>, or a feature-specific
            ///  reason.</param>
            public void OnError( ErrorReason errorReason )
            {
                // Handle any errors here
            }

            /// <summary>
            /// Indicates a successful update.
            /// </summary>
            public void OnSuccess()
            {
                // Handle the success here
            }
        }

        #endregion Private Classes
    }
}
