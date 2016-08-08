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
using System.Collections.Generic;
using System.IO;
using System.Linq;
using PushTechnology.ClientInterface.Client.Factories;
using PushTechnology.ClientInterface.Client.Features.Control.Topics;
using PushTechnology.ClientInterface.Client.Session;
using PushTechnology.ClientInterface.Client.Topics;
using PushTechnology.ClientInterface.Data.JSON;
using PushTechnology.ClientInterface.IO.CBOR;

namespace Examples {

    /// <summary>
    /// This example shows a control client creating a JSON topic and sending updates to it.
    ///
    /// There will be a topic for each currency for which rates are provided. The topic will be created under the FX
    /// topic - so, for example FX/GBP will contain a map of all rate conversions from the base GBP currency. The rates
    /// are represented as string decimal values (e.g. "12.457").
    ///
    /// The <c>addRates</c> method shows how to create a new rates topic, specifying its initial map of values.
    /// The <c>changeRates</c> method which takes a map shows how to completely replace the set of rates for a currency
    /// with a new map of rates.
    ///
    /// The <c>changeRates</c> method which takes a string shows an alternative mechanism where the new rates are
    /// simply supplied as a JSON string.
    ///
    /// Either of the changeRates methods could be used and after the first usage for any topic the values is cached,
    /// and so subsequent set calls can compare with the last value and send only the differences to the server.
    /// </summary>
    public class ControlClientUpdatingJSONTopics {
        private static readonly string rootTopic = "FX";
        private readonly ISession session;
        private volatile IValueUpdater<IJSON> valueUpdater;
        private readonly ITopicControl topicControl;
        private readonly IJSONDataType jsonDataType = Diffusion.DataTypes.JSON;
        private readonly CBORReader reader;

        /// <summary>
        /// Constructor.
        /// </summary>
        /// <param name="serverUrl">for example "ws://diffusion.example.com:80"</param>
        public ControlClientUpdatingJSONTopics( string serverUrl ) {
            session = Diffusion.Sessions.Principal( "control" ).Password( "password" ).Open( serverUrl );

            topicControl = session.GetTopicControlFeature();

            // Register as an updater for all topics under the root
            session.GetTopicUpdateControlFeature().RegisterUpdateSource( rootTopic,
                new MyTopicUpdateSource( valueUpdater ) );
        }

        /// <summary>
        /// Add a new rates topic.
        /// </summary>
        /// <param name="currency">the base currency</param>
        /// <param name="values">the full map of initial rates values</param>
        /// <param name="callback"> reports outcome</param>
        public void AddRates( string currency, IDictionary<string, string> values,
            ITopicControlAddContextCallback<string> callback ) {
            topicControl.AddTopic( RateTopicName( currency ), TopicType.JSON, MapToJSON( values ), currency, callback );
        }

        /// <summary>
        /// Update and existing rates topic, replacing the rates mappings with a new set of mappings.
        /// </summary>
        /// <param name="currency">the base currency</param>
        /// <param name="values">the mew rates values</param>
        /// <param name="callback">reports outcome</param>
        public void ChangeRates( string currency, IDictionary<string, string> values,
            ITopicUpdaterUpdateContextCallback<string> callback ) {
            if ( valueUpdater == null ) {
                throw new InvalidOperationException( "Not registered as updater" );
            }

            valueUpdater.Update( RateTopicName( currency ), MapToJSON( values ), currency, callback );
        }

        /// <summary>
        /// Update an existing rates topic, replacing the rates mappings with a new set of mappings specified as a JSON
        /// string, for example {"USD":"123.45","HKD":"456.3"}.
        /// </summary>
        /// <param name="currency">the base currency</param>
        /// <param name="jsonString">a JSON string specifying the map of currency rates</param>
        /// <param name="callback">reports outcome</param>
        public void ChangeRates( string currency, string jsonString,
            ITopicUpdaterUpdateContextCallback<string> callback ) {
            if ( valueUpdater == null ) {
                throw new InvalidOperationException( "Not registered as updater" );
            }

            valueUpdater.Update( RateTopicName( currency ), jsonDataType.FromJSONString( jsonString ), currency,
                callback );
        }

        /// <summary>
        /// Convert a given map to a JSON object.
        /// </summary>
        /// <param name="values">the map of values to be converted</param>
        /// <returns></returns>
        private IJSON MapToJSON( IDictionary<string, string> values ) {
            using ( var stream = new MemoryStream() ) {

                var writer = new CBORWriter( stream );

                writer.WriteObject();
                for ( int i = 0; i < values.Count; i++ ) {
                    writer.Write( values.ElementAt( i ).Key );
                    writer.Write( values.ElementAt( i ).Value );
                }
                writer.WriteBreak();

                return jsonDataType.ReadValue( stream.ToArray() );
            }
        }


        /// <summary>
        /// Remove a rates entry (remove its topic) and clear cached value for the topic.
        /// </summary>
        /// <param name="currency">the currency to be removed</param>
        /// <param name="callback">reports the outcome</param>
        public void removeRates( string currency, ITopicControlRemoveContextCallback<string> callback ) {
            var topicName = RateTopicName( currency );
            if ( valueUpdater != null ) {
                valueUpdater.RemoveCachedValues( topicName );
            }

            topicControl.RemoveTopics( RateTopicName( currency ), currency, callback );
        }

        /// <summary>
        /// Close the session.
        /// </summary>
        public void close() {
            // Remove our topics and close session when done
            topicControl.RemoveTopics( rootTopic, new MyTopicControlRemoveCallback(session) );
        }

        /// <summary>
        /// Generate a hierarchical topic name for a rates topic. e.g. for currency=GBP would return "FX/GBP".
        /// </summary>
        /// <param name="currency"></param>
        /// <returns>the topic name</returns>
        private static string RateTopicName( string currency ) {
            if ( currency == null ) {
                throw new ArgumentNullException( currency );
            }
            return String.Format( "%s/%s", rootTopic, currency );
        }

        private class MyTopicUpdateSource : TopicUpdateSourceDefault {
            private IValueUpdater<IJSON> valueUpdater;

            public MyTopicUpdateSource( IValueUpdater<IJSON> valueUpdater ) {
                this.valueUpdater = valueUpdater;
            }

            public override void OnActive( string topicPath, ITopicUpdater updater ) {
                valueUpdater = updater.ValueUpdater<IJSON>();
            }
        }

        private class MyTopicControlRemoveCallback : ITopicControlRemoveCallback {
            private ISession session;

            public MyTopicControlRemoveCallback( ISession session ) {
                this.session = session;
            }

            public void OnDiscard() {
                session.Close();
            }

            public void OnTopicsRemoved() {
                session.Close();
            }
        }
    }
}