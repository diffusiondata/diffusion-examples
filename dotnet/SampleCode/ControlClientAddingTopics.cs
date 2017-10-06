/**
 * Copyright © 2014, 2016 Push Technology Ltd.
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
using PushTechnology.ClientInterface.Client.Callbacks;
using PushTechnology.ClientInterface.Client.Content;
using PushTechnology.ClientInterface.Client.Content.Metadata;
using PushTechnology.ClientInterface.Client.Factories;
using PushTechnology.ClientInterface.Client.Features.Control.Topics;
using PushTechnology.ClientInterface.Client.Session;
using PushTechnology.ClientInterface.Client.Topics;

namespace Examples {
    /// <summary>
    /// An example of using a control client to add topics.
    ///
    /// This uses the <see cref="ITopicControl"/> feature only.
    ///
    /// To add or remove topics, the client session must have the <see cref="TopicPermission.MODIFY_TOPIC"/> permission
    /// for that branch of the topic tree.
    /// </summary>
    public class ControlClientAddingTopics {
        private readonly ISession session;
        private readonly ITopicControl topicControl;

        public ControlClientAddingTopics() {
            session = Diffusion.Sessions.Principal( "control" ).Password( "password" )
                .Open( "ws://diffusion.example.com:80" );

            topicControl = session.TopicControl;
        }

        /// <summary>
        /// Adds a topic with the type derived from the value.
        ///
        /// This uses the simple convenience method for adding topics where the topic type and metadata are derived
        /// from a supplied value which can be any object. For example, an integer would result in a single value topic
        /// of type 'integer'.
        /// </summary>
        /// <typeparam name="T">The value type.</typeparam>
        /// <param name="topicPath">The full topic path.</param>
        /// <param name="initialValue">An optional initial value for the topic.</param>
        /// <param name="context">This will be passed back to the callback when reporting success or failure of the
        /// topic add.</param>
        /// <param name="callback">To notify the result of the operation.</param>
        /// <returns>The topic details used to add the topic.</returns>
        public ITopicDetails AddTopicForValue<T>( string topicPath, T initialValue, string context,
            ITopicControlAddContextCallback<string> callback ) {
            return topicControl.AddTopicFromValue( topicPath, initialValue, context, callback );
        }

        /// <summary>
        /// Add a record topic from a list of initial values.
        ///
        /// This demonstrates the simplest mechanism for adding a record topic by supplying values that both the
        /// metadata and the initial values are derived from.
        /// </summary>
        /// <param name="topicPath">The full topic path.</param>
        /// <param name="initialValues">The initial values for the topic fields which will also be used to derive the
        /// metadata definition of the topic.</param>
        /// <param name="context">This will be passed back to the callback when reporting success or failure of the
        /// topic add.</param>
        /// <param name="callback">To notify the result of the operation.</param>
        /// <returns></returns>
        public ITopicDetails AddRecordTopic( string topicPath, List<string> initialValues, string context,
            ITopicControlAddContextCallback<string> callback ) {
            return topicControl.AddTopicFromValue( topicPath,
                Diffusion.Content.NewBuilder<IRecordContentBuilder>().PutFields( initialValues.ToArray() ).Build(),
                context, callback );
        }

        /// <summary>
        /// Adds a record topic with supplied metadata and optional initial content.
        ///
        /// This example shows details being created and would be fine when creating topics that are all different, but
        /// if creating many record topics with the same details, then it is far more efficient to pre-create the
        /// details.
        /// </summary>
        /// <param name="topicPath">The full topic path.</param>
        /// <param name="metadata">The pre-created record metadata.</param>
        /// <param name="initialValue">The optional initial value for the topic which must have been created to match
        /// the supplied metadata.</param>
        /// <param name="context">The context passed back to the callback when the topic is created.</param>
        /// <param name="callback">To notify the result of the operation.</param>
        public void AddRecordTopic( string topicPath, IMContent metadata, IContent initialValue, string context,
            ITopicControlAddContextCallback<string> callback ) {
            var details = topicControl.CreateDetailsBuilder<IRecordTopicDetailsBuilder>().Metadata( metadata ).Build();

            topicControl.AddTopic( topicPath, details, initialValue, context, callback );
        }

        /// <summary>
        /// Remove a single topic given its path.
        /// </summary>
        /// <param name="topicPath">The topic path.</param>
        /// <param name="callback">Notifies the result of the operation.</param>
        public void RemoveTopic( string topicPath, ITopicControlRemovalCallback callback ) {
            topicControl.Remove( "?" + topicPath + "//", callback );
        }

        /// <summary>
        /// Remove one or more topics using a topic selector expression.
        /// </summary>
        /// <param name="topicSelector">The selector expression.</param>
        /// <param name="callback">Notifies the result of the operation.</param>
        public void RemoveTopics( string topicSelector, ITopicControlRemovalCallback callback ) {
            topicControl.Remove( topicSelector, callback );
        }

        /// <summary>
        /// Request that the topic path and its descendants be removed when the session is closed (either explicitly
        /// using <see cref="ISession.Close"/>, or by the server). If more than one session calls this method for the
        /// same topic path, the topics will be removed when the last session is closed.
        ///
        /// Different sessions may call this method for the same topic path, but not for topic paths above or below
        /// existing registrations on the same branch of the topic tree.
        /// </summary>
        /// <param name="topicPath">The part of the topic tree to remove when the last session is closed.</param>
        public void RemoveTopicsWithSession( string topicPath ) {
            topicControl.RemoveTopicsWithSession( topicPath, new DefaultTopicTreeHandler() );
        }

        /// <summary>
        /// Close the session.
        /// </summary>
        public void Close() {
            session.Close();
        }
    }
}