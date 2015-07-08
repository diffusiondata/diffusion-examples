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

namespace UCIStack.Examples
{
    /// <summary>
    /// This demonstrates using a client to subscribe and unsubscribe other clients to topics.
    /// 
    /// This uses the <see cref="ISubscriptionControl"/> feature.
    /// </summary>
    public class ControlClientSubscriptionControl
    {
        #region Fields

        private readonly ISession session;
        private readonly ISubscriptionControl subscriptionControl;

        #endregion Fields

        #region Constructor

        /// <summary>
        /// Constructor.
        /// </summary>
        public ControlClientSubscriptionControl()
        {
            session = Diffusion.Sessions.Principal( "control" ).Password( "password" )
                .Open( "ws://diffusion.example.com:80" );

            subscriptionControl = session.GetSubscriptionControlFeature();
        }

        #endregion Constructor

        #region Public Methods

        /// <summary>
        /// Subscribe a client to topics.
        /// </summary>
        /// <param name="sessionId">The session id of the client to subscribe.</param>
        /// <param name="topicSelector">The topic selector expression.</param>
        /// <param name="callback">The callback for the subscription result.</param>
        public void Subscribe( SessionId sessionId, string topicSelector, ISubscriptionCallback callback )
        {
            // To subscribe a client to a topic, this client session must have the MODIFY_SESSION permission.
            subscriptionControl.Subscribe( sessionId, topicSelector, callback );
        }

        /// <summary>
        /// Unsubscribe a client from topics.
        /// </summary>
        /// <param name="sessionId">The session id of the client to unsubscribe.</param>
        /// <param name="topicSelector">The topic selector expression.</param>
        /// <param name="callback">The callback for the unsubscription result.</param>
        public void Unsubscribe( SessionId sessionId, string topicSelector, ISubscriptionCallback callback )
        {
            subscriptionControl.Unsubscribe( sessionId, topicSelector, callback );
        }

        /// <summary>
        /// Close the session.
        /// </summary>
        public void Close()
        {
            session.Close();
        }

        #endregion Public Methods
    }
}