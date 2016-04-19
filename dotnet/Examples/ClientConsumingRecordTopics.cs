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
using PushTechnology.ClientInterface.Client.Content;
using PushTechnology.ClientInterface.Client.Content.Metadata;
using PushTechnology.ClientInterface.Client.Factories;
using PushTechnology.ClientInterface.Client.Features;
using PushTechnology.ClientInterface.Client.Session;
using PushTechnology.ClientInterface.Client.Types;

namespace UCIStack.Examples
{
    /// <summary>
    /// This demonstrates a client consuming record topics and reading the content using a structured reader.
    /// 
    /// This makes use of the 'Topics' feature only.
    /// 
    /// To subscribe to a topic, the client session must have the 'read_topic' permission for that branch of the topic
    /// tree.
    /// 
    /// This example receives updates to currency conversion rates via a branch of the topic tree where the root topic
    /// is called 'FX'; beneath which is a topic for each base currency, and beneath each of those is a topic for each
    /// target currency which contains the bid and ask rates.  So a topic 'FX/GBP/USD' would contain the rates for GBP
    /// to USD.
    /// 
    /// This example maintains a local dictionary of the rates and also notifies a listener of any rates changes.
    /// 
    /// The example shows the use of empty fields.  Any of the rates can be empty (meaning the rate is not available in
    /// this example), so it can be an empty string in the topic value.  Because delta updates use a zero-length string
    /// to indicate that a field has not changed, a special 'empty field' value is used to indicate that the field has
    /// changed to empty in deltas.  The client application must therefore convert empty string values to "" for the
    /// local rate value.
    /// </summary>
    public class ClientConsumingRecordTopics
    {
        #region Fields

        private static readonly object SyncLock = new object();

        private const string RootTopic = "FX";

        private static readonly Dictionary<string, Currency> Currencies = new Dictionary<string, Currency>();

        private static IRatesListener _listener;

        private readonly ISession session;

        #endregion Fields

        #region Constructor

        public ClientConsumingRecordTopics( string serverUrl, IRatesListener listener )
        {
            _listener = listener;

            session =
                Diffusion.Sessions.Principal( "client" )
                .Password( "password" )
                .Open( serverUrl );

            // Create the record metadata for the rates topic.  It has two decimal fields which are maintained to 5
            // decimal places and allow empty values.
            var mf = Diffusion.Metadata;
            var recordMetadata = mf.RecordBuilder( "Rates" )
                .Add( mf.DecimalBuilder( "Bid" ).SetScale( 5 ).SetAllowsEmpty( true ).Build() )
                .Add( mf.DecimalBuilder( "Ask" ).SetScale( 5 ).SetAllowsEmpty( true ).Build() )
                .Build();

            // Use the Topics feature to add a topic stream and subscribe to all topics under the root.
            var topics = session.GetTopicsFeature();
            var topicSelector = string.Format( "?{0}//", RootTopic );

            topics.AddTopicStream( topicSelector, new RatesTopicStream( recordMetadata ) );

            topics.Subscribe( topicSelector, new TopicsCompletionCallbackDefault() );
        }

        #endregion Constructor

        #region Public Methods

        /// <summary>
        /// Returns the rates for a given base and target currency.
        /// </summary>
        /// <param name="currency">The base currency.</param>
        /// <param name="targetCurrency">The target currency.</param>
        /// <returns>The rates, or null if there is no such base or target currency.</returns>
        public Rates GetRates( string currency, string targetCurrency )
        {
            lock( SyncLock )
            {
                Currency currencyObject;

                return Currencies.TryGetValue( currency, out currencyObject ) 
                    ? currencyObject.GetRates( targetCurrency ) : null;
            }
        }

        /// <summary>
        /// This is used to apply topic stream updates to the local dictionary.
        /// </summary>
        /// <param name="type">The update may be a snapshot or a delta.</param>
        /// <param name="currency">The base currency.</param>
        /// <param name="targetCurrency">The target currency.</param>
        /// <param name="bid">The bid rate.</param>
        /// <param name="ask">The ask rate.</param>
        public static void ApplyUpdate( 
            TopicUpdateType type, 
            string currency, 
            string targetCurrency, 
            string bid, 
            string ask )
        {
            Currency currencyObject;

            lock( SyncLock )
            {
                if( Currencies.ContainsKey( currency ) )
                {
                    currencyObject = Currencies[currency];
                }
                else
                {
                    currencyObject = new Currency();

                    Currencies.Add( currency, currencyObject );
                }
            }

            var rates = type == TopicUpdateType.SNAPSHOT ? currencyObject.SetRate( targetCurrency, bid, ask ) 
                : currencyObject.UpdateRate( targetCurrency, bid, ask );

            _listener.OnNewRate( currency, targetCurrency, rates.BidRate, rates.AskRate );
        }

        /// <summary>
        /// This is used by the topic stream when notified of the unsubscription from a base currency topic.
        /// 
        /// It will remove the base currency and all of its rates from the local dictionary.
        /// </summary>
        /// <param name="currency">The currency to remove.</param>
        public static void RemoveCurrency( string currency )
        {
            lock( SyncLock )
            {
                Currency oldCurrency;
                if( !Currencies.TryGetValue( currency, out oldCurrency ) ) return;

                foreach( var targetCurrency in oldCurrency.CurrencyRates.Keys )
                {
                    _listener.OnRateRemoved( currency, targetCurrency );
                }

                Currencies.Remove( currency );
            }
        }

        /// <summary>
        /// This is used by the topic stream when notification of the unsubscription from a target currency topic.
        /// 
        /// It will remove the rates for the target currency under the base currency.
        /// </summary>
        /// <param name="currency">The base currency.</param>
        /// <param name="targetCurrency">The target currency.</param>
        public static void RemoveRate( string currency, string targetCurrency )
        {
            lock( SyncLock )
            {
                Currency currencyObject;

                if( !Currencies.TryGetValue( currency, out currencyObject ) ) return;

                if( currencyObject.CurrencyRates.Remove( targetCurrency ) )
                {
                    _listener.OnRateRemoved( currency, targetCurrency );
                }
            }
        }

        /// <summary>
        /// Close session.
        /// </summary>
        public void Close()
        {
            lock( SyncLock )
            {
                Currencies.Clear();

                session.Close();
            }
        }

        #endregion Public Methods

        #region Helper Classes

        /// <summary>
        /// Encapsulates a base currency and all of its knownn rates.
        /// </summary>
        public class Currency
        {
            #region Properties

            public Dictionary<string, Rates> CurrencyRates { get; private set; }

            #endregion Properties

            #region Constructor

            public Currency()
            {
                CurrencyRates = new Dictionary<string, Rates>();
            }

            #endregion Constructor

            #region Public Methods

            /// <summary>
            /// Retrieve the rates of a given currency.
            /// </summary>
            /// <param name="currency">The currency.</param>
            /// <returns>The rates of the given currency.</returns>
            public Rates GetRates( string currency )
            {
                return CurrencyRates[currency];
            }

            /// <summary>
            /// Set a currency rate.
            /// </summary>
            /// <param name="currency">The currency to add.</param>
            /// <param name="bid">The 'bid' rate.</param>
            /// <param name="ask">The 'ask' rate.</param>
            public Rates SetRate( string currency, string bid, string ask )
            {
                var newRates = new Rates( bid, ask );

                CurrencyRates.Add( currency, newRates );

                return newRates;
            }

            /// <summary>
            /// Update a currency rate.
            /// </summary>
            /// <param name="currency">The currency to update.</param>
            /// <param name="bid">The 'bid' rate.</param>
            /// <param name="ask">The 'ask' rate.</param>
            public Rates UpdateRate( string currency, string bid, string ask )
            {
                var newRates = CurrencyRates[currency].Update( bid, ask );

                CurrencyRates.Add( currency, newRates );

                return newRates;
            }

            ///// <summary>
            ///// Remove a currency rate.
            ///// </summary>
            ///// <param name="currency">The currency rate to remove.</param>
            //public void RemoveRate( string currency )
            //{
            //    rates.Remove( currency );
            //}

            #endregion Public Methods
        }

        public class Rates
        {
            #region Properties

            /// <summary>
            /// Returns the bid rate, or an empty string if not available.
            /// </summary>
            public string BidRate { get; private set; }

            /// <summary>
            /// Returns the ask rate, or an empty string if not available.
            /// </summary>
            public string AskRate { get; private set; }

            #endregion Properties

            #region Constructor

            /// <summary>
            /// Constructor.
            /// </summary>
            /// <param name="bid">The 'bid' rate, or an empty string.</param>
            /// <param name="ask">The 'ask' rate, or an empty string.</param>
            public Rates( string bid, string ask )
            {
                BidRate = bid;
                AskRate = ask;
            }

            #endregion Constructor

            #region Public Methods

            public Rates Update( string bid, string ask )
            {
                string newBid;

                if( string.Empty.Equals( bid ) )
                {
                    newBid = BidRate;
                }
                else if( Constants.EMPTY_FIELD_STRING.Equals( bid ) )
                {
                    newBid = string.Empty;
                }
                else
                {
                    newBid = bid;
                }

                string newAsk;

                if( string.Empty.Equals( ask ) )
                {
                    newAsk = AskRate;
                }
                else if( Constants.EMPTY_FIELD_STRING.Equals( ask ) )
                {
                    newAsk = string.Empty;
                }
                else
                {
                    newAsk = ask;
                }

                return new Rates( newBid, newAsk );
            }

            #endregion Public Methods
        }

        /// <summary>
        /// A listener for <see cref="Rates"/> updates.
        /// </summary>
        public interface IRatesListener
        {
            /// <summary>
            /// Notification of a new rate or rate update.
            /// </summary>
            /// <param name="currency">The base currency.</param>
            /// <param name="targetCurrency">The target currency.</param>
            /// <param name="bid">The bid rate, or an empty string if not available.</param>
            /// <param name="ask">The ask rate, or an empty string if not available</param>
            void OnNewRate( string currency, string targetCurrency, string bid, string ask );

            /// <summary>
            /// Notification of a rate being removed.
            /// </summary>
            /// <param name="currency">The base currency.</param>
            /// <param name="targetCurrency">The target currency.</param>
            void OnRateRemoved( string currency, string targetCurrency );
        }

        private class RatesTopicStream : TopicStreamDefault
        {
            #region Fields

            private readonly IMRecord metadata;

            #endregion Fields

            #region Constructor

            /// <summary>
            /// Constructor.
            /// </summary>
            /// <param name="metadata">The metadata.</param>
            public RatesTopicStream( IMRecord metadata )
            {
                this.metadata = metadata;
            }

            #endregion Constructor

            #region Overrides

            /// <summary>
            /// Topic update received.
            /// This indicates an update to the state of a topic that is subscribed to.
            /// </summary>
            /// <param name="topicPath">the full topic path.</param>
            /// <param name="content">the topic content. The context may contain more
            /// information about the nature of the content</param>
            /// <param name="context">the update context which may indicate whether the content represents the total
            ///  state or a change to the state</param>
            public override void OnTopicUpdate( string topicPath, IContent content, IUpdateContext context )
            {
                var topicElements = topicPath.Split( '/' );

                // It is only a rate if topic name has 3 elements in path
                if( topicElements.Length != 3 ) return;

                var record = Diffusion.Content.NewReader<IRecordContentReader>( content ).NextRecord();
                var reader = record.CreateNewReader( metadata, Constants.EMPTY_FIELD_STRING );

                ApplyUpdate(
                    context.UpdateType,
                    topicElements[1],
                    topicElements[2],
                    reader.Get( "Bid" ),
                    reader.Get( "Ask" ) );
            }

            /// <summary>
            /// This notifies when a topic is unsubscribed.
            /// </summary>
            /// <param name="topicPath">the full topic path.</param>
            /// <param name="reason">the reason for unsubscription.</param>
            public override void OnUnsubscription( string topicPath, TopicUnsubscribeReason reason )
            {
                var topicElements = topicPath.Split( '/' );

                switch( topicElements.Length )
                {
                    case 3:
                    {
                        RemoveRate( topicElements[1], topicElements[2] );
                    }
                        break;

                    case 2:
                    {
                        RemoveCurrency( topicElements[1] );
                    }
                        break;
                }
            }

            #endregion Overrides
        }

        #endregion Helper Classes
    }
}