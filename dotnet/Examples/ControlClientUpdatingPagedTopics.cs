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
using System.Collections.Generic;
using PushTechnology.ClientInterface.Client.Factories;
using PushTechnology.ClientInterface.Client.Features.Control.Topics;
using PushTechnology.ClientInterface.Client.Session;
using PushTechnology.ClientInterface.Client.Topics;
using PushTechnology.ClientInterface.Client.Topics.Update;

namespace Examples
{
    /// <summary>
    /// An example of using a control client to create and update paged topics.
    /// 
    /// This uses the <see cref="ITopicControl"/> feature to create a paged topic and the <see cref="ITopicUpdateControl"/>
    /// feature to send updates to it.
    /// 
    /// This demonstrates some simple examples of paged topic updates but not all of the possible ways in which they
    /// can be done.
    /// 
    /// To send updates to a topic, the client session requires the UPDATE_TOPIC permission for that branch of the
    /// topic tree.
    /// </summary>
    public class ControlClientUpdatingPagedTopics
    {
        #region Fields

        private const string OrderedTopic = "Paged/Ordered";
        private const string UnorderedTopic = "Paged/Unordered";

        private readonly ISession session;
        private readonly ITopicControl topicControl;
        private readonly IPagedRecordOrderedUpdateFactory orderedUpdateFactory;
        private readonly IPagedStringUnorderedUpdateFactory unorderedUpdateFactory;
        private static ITopicUpdater _pagedUpdater;

        #endregion Fields

        #region Constructor

        /// <summary>
        /// Constructor.
        /// </summary>
        public ControlClientUpdatingPagedTopics()
        {
            session = Diffusion.Sessions.Principal( "control" ).Password( "password" )
                .Open( "ws://diffusion.example.com:80" );

            topicControl = session.GetTopicControlFeature();
            var updateControl = session.GetTopicUpdateControlFeature();

            orderedUpdateFactory = updateControl.UpdateFactory<IPagedRecordOrderedUpdateFactory>();
            unorderedUpdateFactory = updateControl.UpdateFactory<IPagedStringUnorderedUpdateFactory>();

            var metadata = Diffusion.Metadata;

            // Create an unordered paged string topic
            topicControl.AddTopic(
                UnorderedTopic,
                topicControl.NewDetails( TopicType.PAGED_STRING ),
                new TopicControlAddCallbackDefault() );

            // Create an ordered paged record topic
            var recordMetadata = metadata.Record(
                "Record",
                metadata.String( "Name" ),
                metadata.String( "Address" ) );

            topicControl.AddTopic(
                OrderedTopic,
                topicControl
                    .CreateDetailsBuilder<IPagedRecordTopicDetailsBuilder>()
                    .Metadata( recordMetadata )
                    .Order( new PagedRecordOrderKey( "Name" ) ).Build(),
                new TopicControlAddCallbackDefault() );

            // Register an updater for topics under the 'Paged' branch
            updateControl.RegisterUpdateSource( "Paged", new UpdateSource() );
        }

        #endregion Constructor

        #region Public Methods

        /// <summary>
        /// Add a new line from an ordered topic.
        /// </summary>
        /// <param name="name">The name field value.</param>
        /// <param name="address">The address field value.</param>
        /// <param name="callback">The callback to notify the result.</param>
        public void AddOrdered( string name, string address, ITopicUpdaterUpdateCallback callback )
        {
            Update(
                OrderedTopic,
                orderedUpdateFactory.Add( Diffusion.Content.NewRecord( name, address ) ),
                callback );
        }

        /// <summary>
        /// Update a line of an ordered topic.
        /// </summary>
        /// <param name="name">The name of the line to update.</param>
        /// <param name="address">The new address field value.</param>
        /// <param name="callback">The callback to notify the result.</param>
        public void UpdateOrdered( string name, string address, ITopicUpdaterUpdateCallback callback )
        {
            Update(
                OrderedTopic,
                orderedUpdateFactory.Update( Diffusion.Content.NewRecord( name, address ) ),
                callback );
        }

        /// <summary>
        /// Remove a line from an ordered topic.
        /// </summary>
        /// <param name="name">The name of the line to remove.</param>
        /// <param name="callback">The callback to notify the result.</param>
        public void RemoveOrdered( string name, ITopicUpdaterUpdateCallback callback )
        {
            Update(
                OrderedTopic,
                orderedUpdateFactory.Remove( Diffusion.Content.NewRecord( name, "" ) ),
                callback );
        }

        /// <summary>
        /// Add a line or lines to the end of an unordered topic.
        /// </summary>
        /// <param name="values">The lines to add.</param>
        /// <param name="callback">The callback to notify the result.</param>
        public void AddUnordered( ICollection<string> values, ITopicUpdaterUpdateCallback callback )
        {
            Update(
                UnorderedTopic,
                unorderedUpdateFactory.Add( values ),
                callback );
        }

        /// <summary>
        /// Insert a line or lines at a specified index within an unordered topic.
        /// </summary>
        /// <param name="index">The index at which to add the line.</param>
        /// <param name="values">The lines to insert.</param>
        /// <param name="callback">The callback to notify the result.</param>
        public void InsertUnordered( int index, ICollection<string> values, ITopicUpdaterUpdateCallback callback )
        {
            Update(
                UnorderedTopic,
                unorderedUpdateFactory.Insert( index, values ),
                callback );
        }

        /// <summary>
        /// Remove a specific line from an unordered topic.
        /// </summary>
        /// <param name="index">The index of the line to remove.</param>
        /// <param name="callback">The callback to notify the result.</param>
        public void RemoveUnordered( int index, ITopicUpdaterUpdateCallback callback )
        {
            Update(
                UnorderedTopic,
                unorderedUpdateFactory.Remove( index ),
                callback );
            
        }

        /// <summary>
        /// Update a line within an unordered topic.
        /// </summary>
        /// <param name="index">The index of the line to update.</param>
        /// <param name="value">The new line value.</param>
        /// <param name="callback">The callback to notify the result.</param>
        public void UpdateUnordered( int index, string value, ITopicUpdaterUpdateCallback callback )
        {
            Update(
                OrderedTopic,
                unorderedUpdateFactory.Update( index, value ),
                callback );
        }

        /// <summary>
        /// Close the session.
        /// </summary>
        public void Close()
        {
            // Remove our topics and close the session when done.
            topicControl.RemoveTopics( ">Paged", new RemoveCallback( session ) );
        }

        #endregion Public Methods

        #region Private Methods

        private static void Update( string topic, IUpdate update, ITopicUpdaterUpdateCallback callback )
        {
            if( _pagedUpdater == null )
            {
                throw new InvalidOperationException( "The paged updater has not been initialised." );
            }

            _pagedUpdater.Update( topic, update, callback );
        }

        #endregion Private Methods

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

        private class UpdateSource : TopicUpdateSourceDefault
        {
            #region Overrides

            /// <summary>
            /// State notification that this source is now active for the specified topic path, and is therefore in a valid
            /// state to send updates on topics at or below the registered topic path.
            /// </summary>
            /// <param name="topicPath">The registration path.</param>
            /// <param name="updater">An updater that may be used to update topics at or below the registered path.</param>
            public override void OnActive( string topicPath, ITopicUpdater updater )
            {
                _pagedUpdater = updater;
            }

            #endregion Overrides
        }

        #endregion Private Classes
    }
}