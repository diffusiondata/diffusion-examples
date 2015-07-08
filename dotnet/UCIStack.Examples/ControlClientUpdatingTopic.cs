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

using PushTechnology.ClientInterface.Client.Factories;
using PushTechnology.ClientInterface.Client.Features.Control.Topics;
using PushTechnology.ClientInterface.Client.Session;
using PushTechnology.ClientInterface.Client.Topics;

namespace UCIStack.Examples
{
    /// <summary>
    /// An example of using a control client to create and update a topic in non-exclusive mode (as opposed to acting
    /// as an exclusive update source).  In this mode other clients could update the same topic (on a 'last update wins'
    /// basis).
    /// 
    /// This uses the <see cref="ITopicControl"/> feature to create a topic and the <see cref="ITopicUpdateControl"/> feature
    /// to send updates to it.
    /// 
    /// To send updates to a topic, the client session requires the 'update_topic' permission for that branch of the
    /// topic tree.
    /// </summary>
    public class ControlClientUpdatingTopic
    {
        #region Fields

        private const string Topic = "MyTopic";

        private readonly ISession session;
        private readonly ITopicControl topicControl;
        private readonly ITopicUpdateControl updateControl;

        #endregion Fields

        #region Constructor

        /// <summary>
        /// Constructor.
        /// </summary>
        public ControlClientUpdatingTopic()
        {
            session = Diffusion.Sessions.Principal( "control" )
                .Password( "password" )
                .Open( "ws://diffusion.example.com:80" );

            topicControl = session.GetTopicControlFeature();
            updateControl = session.GetTopicUpdateControlFeature();

            // Create a single-value topic.
            topicControl.AddTopicFromValue( Topic, TopicType.SINGLE_VALUE, new TopicControlAddCallbackDefault() );
        }

        #endregion Constructor

        #region Public Methods

        /// <summary>
        /// Update the topic with a string value.
        /// </summary>
        /// <param name="value">The update value.</param>
        /// <param name="callback">The update callback.</param>
        public void Update( string value, ITopicUpdaterUpdateCallback callback )
        {
            updateControl.Updater.Update( Topic, value, callback );
        }

        /// <summary>
        /// Close the session.
        /// </summary>
        public void Close()
        {
            // Remove our topic and close session when done.
            topicControl.RemoveTopics( ">" + Topic, new RemoveCallback( session ) );
        }

        #endregion Public Methods

        #region Private Classes

        private class RemoveCallback : TopicControlRemoveCallbackDefault
        {
            #region Fields

            private readonly ISession theSession;

            #endregion Fields

            #region Constructor

            public RemoveCallback( ISession session )
            {
                theSession = session;
            }

            #endregion Constructor

            #region Overrides

            /// <summary>
            /// Notification that a call context was closed prematurely, typically due to a timeout or the session being
            /// closed.  No further calls will be made for the context.
            /// </summary>
            public override void OnDiscard()
            {
                theSession.Close();
            }

            /// <summary>
            /// Topic(s) have been removed.
            /// </summary>
            public override void OnTopicsRemoved()
            {
                theSession.Close();
            }

            #endregion Overrides
        }

        #endregion Private Classes
    }
}