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

using System.Collections.Generic;
using PushTechnology.ClientInterface.Client.Factories;
using PushTechnology.ClientInterface.Client.Features.Control.Clients;
using PushTechnology.ClientInterface.Client.Features.Control.Topics;
using PushTechnology.ClientInterface.Client.Session;

namespace UCIStack.Examples
{
    /// <summary>
    /// This is an example of a control client using both the client control and the subscription control feature to
    /// monitor and subscribe clients.
    /// 
    /// The example shows a control client that wants all clients that are in Italy and are in the 'Accounts'
    /// department (determined by an additional property) to be subscribed to the 'ITAccounts' topic.
    /// 
    /// It also has a method which makes use of filtered subscription to change the topic that all matching clients
    /// are subscribed to.
    /// </summary>
    public class ControlClientUsingSessionProperties
    {
        #region Fields

        private readonly ISession _session;
        private readonly IClientControl _clientControl;
        private readonly ISubscriptionControl _subscriptionControl;

        private const string CurrentTopic = "ITAccounts";

        private readonly ISubscriptionCallback _subscriptionCallback = 
            new SubscriptionCallbackDefault();

        private readonly ISubscriptionByFilterCallback _subscriptionFilterCallback = 
            new SubscriptionByFilterCallbackDefault();

        #endregion Fields

        #region Constructor

        public ControlClientUsingSessionProperties()
        {
            _session = Diffusion.Sessions.Principal( "control" )
                .Password( "password" )
                .Open( "ws.//diffusion.example.com:80" );

            _clientControl = _session.GetClientControlFeature();
            _subscriptionControl = _session.GetSubscriptionControlFeature();

            // Configure a listener which will be notified firstly of all open client sessions and then of all that
            // subsequently open.  All that are in the Italian accounts department get subscribed to the current topic.
            // Only the country and department properties are requested.

            // Set up a listener to receive notification of all sessions
            _clientControl.SetSessionPropertiesListener( 
                new PropertiesListener( 
                    _subscriptionControl, CurrentTopic, _subscriptionCallback ), "$Country", "Department" );
        }

        #endregion Constructor

        #region Public Methods

        /// <summary>
        /// This can be used to change the topic that all of the Italian accounts department is subscribed to.  It will
        /// unsubscribe all current clients from the old topic and subscribe them to the new one.  All new clients will
        /// be subscribed to the new one.
        /// </summary>
        /// <param name="topic"></param>
        public void ChangeTopic( string topic )
        {
            const string oldTopic = CurrentTopic;

            // Change the topic that all new clients will get
            const string filter = "Department is 'Accounts' and $Country is 'IT'";

            // Unsubscribe all from the old topic
            _subscriptionControl.UnsubscribeByFilter( filter, oldTopic, _subscriptionFilterCallback );

            // And subscribe all to the new topic
            _subscriptionControl.SubscribeByFilter( filter, CurrentTopic, _subscriptionFilterCallback );
        }

        /// <summary>
        /// Close the session.
        /// </summary>
        public void Close()
        {
            _session.Close();
        }

        #endregion Public Methods

        #region Private Classes

        /// <summary>
        /// This session properties listener will be notified firstly of all open client sessions and then of all that
        /// subsequently open.  All that are in the Italian Accounts department get subscribed to the current topic.
        /// </summary>
        private class PropertiesListener : SessionPropertiesListenerDefault
        {
            #region Fields

            private readonly ISubscriptionControl _subscriptionControl;
            private readonly string _currentTopic;
            private readonly ISubscriptionCallback _subscriptionCallback;

            #endregion Fields

            #region Constructor

            public PropertiesListener( 
                ISubscriptionControl subscriptionControl, 
                string currentTopic, 
                ISubscriptionCallback subscriptionCallback )
            {
                _subscriptionControl = subscriptionControl;
                _currentTopic = currentTopic;
                _subscriptionCallback = subscriptionCallback;
            }

            #endregion Constructor

            /// <summary>
            /// Notification that a new client session has been opened.
            /// When the listener is registered, this will be called for all existing sessions.  It will then be called for
            /// every new session that opens whilst the listener is registered.
            /// This will be called for every client session regardless of requested session properties.
            /// </summary>
            /// <param name="sessionId">The session identifier.</param><param name="properties">The map of requested session property values.  This can be empty if no properties
            /// were requested.  If a requested property did not exist then it will not be prsent in the map.</param>
            public override void OnSessionOpen( SessionId sessionId, IDictionary<string, string> properties )
            {
                if( "Accounts".Equals( properties["Department"] ) &&
                        "IT".Equals( properties["$Country"] ) )
                {
                    _subscriptionControl.Subscribe( sessionId, _currentTopic, _subscriptionCallback );
                }
            }
        }

        #endregion Private Classes
    }
}