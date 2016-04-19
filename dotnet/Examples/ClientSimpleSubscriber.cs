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

using PushTechnology.ClientInterface.Client.Callbacks;
using PushTechnology.ClientInterface.Client.Content;
using PushTechnology.ClientInterface.Client.Content.Metadata;
using PushTechnology.ClientInterface.Client.Factories;
using PushTechnology.ClientInterface.Client.Features;
using PushTechnology.ClientInterface.Client.Session;
using PushTechnology.ClientInterface.Client.Topics;
using PushTechnology.ClientInterface.Client.Types;

namespace Examples
{
    /// <summary>
    /// In this simple and commonest case for a client, we just subscribe to a few topics and assign handlers for each
    /// to receive content.
    /// 
    /// This makes use of the <see cref="ITopics"/> feature only.
    /// 
    /// To subscribe to a topic, the client session must have the <see cref="TopicPermission.READ_TOPIC"/> permission
    /// for that branch of the topic tree.
    /// </summary>
    public class ClientSimpleSubscriber
    {
        #region Fields

        private readonly ISession session;

        #endregion Fields

        #region Constructor

        /// <summary>
        /// Constructor.
        /// </summary>
        public ClientSimpleSubscriber()
        {
            session = Diffusion.Sessions.Principal( "client" ).Password( "password" )
                .Open( "ws://diffusion.example.com:80" );

            // Use the Topics feature to add a topic stream for Foo and all topics under Bar and request subscription
            // to those topics
            var topics = session.GetTopicsFeature();

            topics.AddTopicStream( ">Foo", new FooTopicStream() );
            topics.AddTopicStream( ">Bar/", new BarTopicStream() );

            topics.Subscribe( Diffusion.TopicSelectors.AnyOf( "Foo", "Bar//" ), new TopicsCompletionCallbackDefault() );
        }

        #endregion Constructor

        #region Public Methods

        /// <summary>
        /// Close the session.
        /// </summary>
        public void Close()
        {
            session.Close();
        }

        #endregion Public Methods

        #region Private Classes

        /// <summary>
        /// The topic stream for all messages on the 'Foo' topic.
        /// </summary>
        private class FooTopicStream : TopicStreamDefault
        {
            /// <summary>
            /// Topic update received.
            /// 
            /// This indicates an update to the state of a topic that is subscribed
            /// to.
            /// </summary>
            /// <param name="topicPath">the full topic path.</param>
            /// <param name="content">the topic content. The context may contain more
            /// information about the nature of the content</param>
            /// <param name="context">the update context which may indicate whether the
            /// content represents the total state or a change to the state</param>
            public override void OnTopicUpdate( string topicPath, IContent content, IUpdateContext context )
            {
            }
        }

        /// <summary>
        /// The topic stream for all messages on 'Bar' topics.
        /// </summary>
        private class BarTopicStream : TopicStreamDefault
        {
            #region Fields

            #endregion Fields

            /// <summary>
            /// Topic update received.
            /// 
            /// This indicates an update to the state of a topic that is subscribed
            /// to.
            /// </summary>
            /// <param name="topicPath">the full topic path.</param>
            /// <param name="content">the topic content. The context may contain more
            /// information about the nature of the content</param>
            /// <param name="context">the update context which may indicate whether the
            /// content represents the total state or a change to the state</param>
            public override void OnTopicUpdate( string topicPath, IContent content, IUpdateContext context )
            {
                var reader = Diffusion.Content.NewReader<IRecordContentReader>( content );

                foreach( var field in reader.NextRecord() )
                {
                    // Log the value of each field here.
                }
            }
        }

        private class TopicStreamDefault : ITopicStream
        {
            /// <summary>
            /// Notification of a contextual error related to this callback. This is
            /// analogous to an exception being raised. Situations in which
            /// <code>OnError</code> is called include the session being closed, a
            /// communication timeout, or a problem with the provided parameters. No
            /// further calls will be made to this callback.
            /// </summary>
            /// <param name="errorReason">errorReason a value representing the error; this can be one of
            /// constants defined in <see cref="ErrorReason" />, or a feature-specific
            /// reason.</param>
            public virtual void OnError( ErrorReason errorReason )
            {
            }

            /// <summary>
            /// Called to notify that a stream context was closed normally.
            /// 
            /// No further calls will be made for the stream context.
            /// </summary>
            public void OnClose()
            {
            }

            /// <summary>
            /// This notifies when a topic is subscribed to.
            /// 
            /// This provides only <see cref="TopicDetailsLevel.BASIC"/>details of the topic.
            /// </summary>
            /// <param name="topicPath">the full topic path</param>
            /// <param name="details">the basic details</param>
            public virtual void OnSubscription( string topicPath, ITopicDetails details )
            {
            }

            /// <summary>
            /// This notifies when a topic is unsubscribed.
            /// </summary>
            /// <param name="topicPath">the full topic path.</param>
            /// <param name="reason">the reason for unsubscription.</param>
            public virtual void OnUnsubscription( string topicPath, TopicUnsubscribeReason reason )
            {
            }

            /// <summary>
            /// Topic update received.
            /// 
            /// This indicates an update to the state of a topic that is subscribed
            /// to.
            /// </summary>
            /// <param name="topicPath">the full topic path.</param>
            /// <param name="content">the topic content. The context may contain more
            /// information about the nature of the content</param>
            /// <param name="context">the update context which may indicate whether the
            /// content represents the total state or a change to the state</param>
            public virtual void OnTopicUpdate( string topicPath, IContent content, IUpdateContext context )
            {
            }
        }

        #endregion Private Classes
    }
}