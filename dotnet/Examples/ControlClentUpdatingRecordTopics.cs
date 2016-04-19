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
using PushTechnology.ClientInterface.Client.Content;
using PushTechnology.ClientInterface.Client.Content.Metadata;
using PushTechnology.ClientInterface.Client.Factories;
using PushTechnology.ClientInterface.Client.Features.Control.Topics;
using PushTechnology.ClientInterface.Client.Session;
using PushTechnology.ClientInterface.Client.Topics;
using PushTechnology.ClientInterface.Client.Topics.Update;
using PushTechnology.ClientInterface.Client.Types;

namespace UCIStack.Examples
{
    /// <summary>
    /// An example of using a control client to create and update a record topic in exclusive mode.
    /// 
    /// This uses the topic control feature to create a topic and the topic update control feature to send updates to
    /// it.
    /// 
    /// Both 'full' and 'patch' updating techniques are demonstrated.  Full updates involve sending the whole topic
    /// state to the server; this will be compared with the current state and a delta of any differences published to
    /// subscribed clients.  With patch updates it is only necessary to send the values of the fields that have changed
    /// to the server where they will be applied to the current topic state and published to subscribers.  The latter
    /// mechanism is not so well suited to this example where there are only 2 fields, but for topics with many fields
    /// this could represent considerable savings in the amount of data sent to the server.
    /// 
    /// To send updates to a topic, the client session requires the 'update_topic' permission for that branch of the
    /// topic tree.
    /// 
    /// The example also demonstrates a simple usage of a structured record builder for generating content as such a
    /// builder validates the input against the metadata.
    /// </summary>
    public class ControlClientUpdatingRecordTopics
    {
        #region Fields

        private const string RootTopic = "FX";

        private readonly ISession session;
        private readonly ITopicControl topicControl;
        private readonly IMRecord recordMetadata;
        private readonly IRecordTopicDetails topicDetails;
        private readonly IContentUpdateFactory updateFactory;
        private readonly IRecordStructuredBuilder deltaRecordBuilder;
        private readonly ITopicUpdater topicUpdater;

        #endregion Fields

        #region Constructor

        /// <summary>
        /// Constructor.
        /// </summary>
        /// <param name="serverUrl">The server url, for example "ws://diffusion.example.com:80".</param>
        public ControlClientUpdatingRecordTopics( string serverUrl )
        {
            session =
                Diffusion.Sessions.Principal( "client" )
                .Password( "password" )
                .Open( serverUrl );

            topicControl = session.GetTopicControlFeature();

            var mf = Diffusion.Metadata;

            // Create the record metadata for the rates topic.  It has 2 decimal fields which are maintained to 5
            // decimal places and allow empty values.
            recordMetadata =
                mf.RecordBuilder( "Rates" )
                    .Add( mf.DecimalBuilder( "Buy" ).SetScale( 5 ).SetAllowsEmpty( true ).Build() )
                    .Add( mf.DecimalBuilder( "Sell" ).SetScale( 5 ).SetAllowsEmpty( true ).Build() )
                    .Build();

            // Create the topic details to be used for all rates topics
            topicDetails =
                topicControl.CreateDetailsBuilder<IRecordTopicDetailsBuilder>()
                    .EmptyFieldValue( Constants.EMPTY_FIELD_STRING )
                    .Metadata( mf.Content( "CurrencyDetails", recordMetadata ) )
                    .Build();

            // Create a delta builder that can be reused for bid-only changes
            deltaRecordBuilder =
                Diffusion.Content.NewDeltaRecordBuilder( recordMetadata )
                    .EmptyFieldValue( Constants.EMPTY_FIELD_STRING );

            var updateControl = session.GetTopicUpdateControlFeature();

            updateFactory = updateControl.UpdateFactory<IContentUpdateFactory>();

            // Register as an updater for all topics under the root
            updateControl.RegisterUpdateSource( RootTopic, new TopicUpdateSource( topicUpdater ) );
        }

        #endregion Constructor

        #region Public Methods

        /// <summary>
        /// Adds a new conversion rate in terms of base currency and target currency.
        /// 
        /// The bid and ask rates are entered as strings which may be a decimal value; this will be parsed and
        /// validated, rounding to 5 decimal places.  If a zero-length string ("") is supplied, the rate will be set to
        /// 'empty' and clients will receive a zero-length string in the initial load.
        /// </summary>
        /// <param name="currency">The base currency (e.g. GBP).</param>
        /// <param name="targetCurrency">The target current (e.g. USD).</param>
        /// <param name="bid">The 'bid' rate.</param>
        /// <param name="ask">The 'ask' rate.</param>
        /// <param name="callback">A callback which will be called to report the outcome.  The context in the callback
        /// wil be currency/target currency (e.g. "GBP/USD").</param>
        public void AddRate(
            string currency,
            string targetCurrency,
            string bid,
            string ask,
            ITopicControlAddContextCallback<string> callback )
        {
            topicControl.AddTopic( RateTopicName( currency, targetCurrency ),
                topicDetails,
                CreateRateContent( bid, ask ),
                string.Format( "{0}/{1}", currency, targetCurrency ),
                callback );
        }

        /// <summary>
        /// Update a rate.
        /// 
        /// The rate in question must have been added first using <see cref="AddRate"/> otherwise this will fail.
        /// 
        /// The bid and ask rates are entered as strings which may be a decimal value; this will be parsed and
        /// validated, rounding to 5 decimal places.  A zero-length string may be supplied to indicate 'no rate
        /// available'.  The server will compare the supplied values with the current values, and if different will
        /// notify clients of a delta of change.  Only changed fields are notified to clients - unchanged fields are
        /// passed as a zero-length string.  If a field has changed to zero length, the client will receive the
        /// special empty field value in the delta.
        /// </summary>
        /// <param name="currency">The base currency.</param>
        /// <param name="targetCurrency">The target currency.</param>
        /// <param name="bid">The new bid rate.</param>
        /// <param name="ask">The new ask rate.</param>
        /// <param name="callback">A callback which will be called to report the outcome.  The context in the callback
        /// will be currency/target currency (e.g. "GBP/USD").</param>
        public void ChangeRate(
            string currency,
            string targetCurrency,
            string bid,
            string ask,
            ITopicUpdaterUpdateContextCallback<string> callback )
        {
            if( topicUpdater == null )
            {
                throw new InvalidOperationException( "Not registered as an updater." );
            }

            topicUpdater.Update( 
                RateTopicName( currency, targetCurrency ),
                updateFactory.Update( CreateRateContent( bid, ask ) ),
                string.Format( "{0}/{1}", currency, targetCurrency ),
                callback );
        }

        /// <summary>
        /// Updates just the 'bid' value for a specified rate.
        /// 
        /// This method demonstrates the alternative 'delta' mechanism of updating.  In this example it does not make
        /// much sense, but for records with many fields where you know only one is changing, this negates the need to
        /// send the whole topic state in each update.
        /// </summary>
        /// <param name="currency">The base currency.</param>
        /// <param name="targetCurrency">The target currency.</param>
        /// <param name="bid">The new bid rate which can be an empty string to set to 'not available'.</param>
        /// <param name="callback">A callback which will be called to report the outcome.  The context in the callback
        /// will be currency/targetCurrency (e.g. "GBP/USD".</param>
        public void ChangeBidRate(
            string currency,
            string targetCurrency,
            string bid,
            ITopicUpdaterUpdateContextCallback<string> callback )
        {
            if( topicUpdater == null )
            {
                throw new InvalidOperationException( "Not registered as an updater." );
            }

            var cf = Diffusion.Content;

            topicUpdater.Update(
                RateTopicName( currency, targetCurrency ),
                updateFactory.Patch(
                    cf.NewBuilder<IRecordContentBuilder>()
                        .PutRecords(
                            deltaRecordBuilder.Set(
                                    "Bid",
                                    "".Equals( bid ) ? Constants.EMPTY_FIELD_STRING : bid )
                                .Build() )
                        .Build() ),
                string.Format( "{0}/{1}", currency, targetCurrency ),
                callback );
        }

        /// <summary>
        /// Remove a rate (removes its topic).
        /// </summary>
        /// <param name="currency">The base currency.</param>
        /// <param name="targetCurrency">The target currency.</param>
        /// <param name="callback">Reports the outcome.</param>
        public void RemoveRate(
            string currency,
            string targetCurrency,
            ITopicControlRemoveContextCallback<string> callback )
        {
            topicControl.RemoveTopics( 
                string.Format( ">{0}", RateTopicName( currency, targetCurrency ) ),
                string.Format( "{0}/{1}", currency, targetCurrency ),
                callback );
        }

        /// <summary>
        /// Removes a currency (removes its topics and all subordinate rate topics).
        /// </summary>
        /// <param name="currency">The base currency.</param>
        /// <param name="callback">Reports the outcome.</param>
        public void RemoveCurrency(
            string currency,
            ITopicControlRemoveContextCallback<string> callback )
        {
            topicControl.RemoveTopics(
                string.Format( ">{0}/{1}", RootTopic, currency ),
                currency,
                callback );
        }

        /// <summary>
        /// Close the session.
        /// </summary>
        public void Close()
        {
            // Remove our topics and close the session when done
            topicControl.RemoveTopics(
                string.Format( ">{0}", RootTopic ),
                new RemoveCallback( session ) );
        }

        #endregion Public Methods

        #region Private Methods

        /// <summary>
        /// Generates a hierarchical topic name for a rate topic.
        /// 
        /// e.g. for currency=GBP and targetCurrency=USD, this would return "FX/GBP/USD".
        /// </summary>
        /// <param name="currency">The base currency.</param>
        /// <param name="targetCurrency">The target currency.</param>
        /// <returns>The topic name.</returns>
        private static string RateTopicName( string currency, string targetCurrency )
        {
            return string.Format( "{0}/{1}/{2}", RootTopic, currency, targetCurrency );
        }

        /// <summary>
        /// Create rate contents for a full update.
        /// </summary>
        /// <param name="bid">The 'bid' rate, or an empty string.</param>
        /// <param name="ask">The 'ask' rate, or an empty string.</param>
        /// <returns></returns>
        private IContent CreateRateContent( string bid, string ask )
        {
            var cf = Diffusion.Content;

            var content =
                cf.NewBuilder<IRecordContentBuilder>()
                    .PutRecords(
                        cf.NewRecordBuilder( recordMetadata )
                            .Set( "Buy", bid )
                            .Set( "Sell", ask )
                            .Build() )
                    .Build();

            return content;
        }

        #endregion Private Methods

        #region Private Classes

        private class RemoveCallback : ITopicControlRemoveCallback
        {
            #region Fields

            private readonly ISession theSession;

            #endregion Fields

            #region Constructor

            public RemoveCallback( ISession session )
            {
                theSession = session;
            }

            #endregion Cosntructor

            /// <summary>
            /// This is called to notify that a call context was closed prematurely, typically due to a timeout or the 
            /// session being closed.  No further calls will be made for the context.
            /// </summary>
            public void OnDiscard()
            {
                theSession.Close();
            }

            /// <summary>
            /// Topic(s) have been removed.
            /// </summary>
            public void OnTopicsRemoved()
            {
                theSession.Close();
            }
        }

        private class TopicUpdateSource : TopicUpdateSourceDefault
        {
            #region Fields

            private ITopicUpdater theUpdater;

            #endregion Fields

            #region Constructor

            public TopicUpdateSource( ITopicUpdater updater )
            {
                theUpdater = updater;
            }

            #endregion Constructor

            #region Overrides

            /// <summary>
            /// State notification that this source is now active for the specified topic path, and is therefore in a valid
            /// state to send updates on topics at or below the registered topic path.
            /// </summary>
            /// <param name="topicPath">The registration path.</param>
            /// <param name="updater">An updater that may be used to update topics at or below the registered path.</param>
            public override void OnActive( string topicPath, ITopicUpdater updater )
            {
                theUpdater = updater;
            }

            #endregion Overrides
        }

        #endregion Private Classes
    }
}