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

using PushTechnology.ClientInterface.Client.Content;
using PushTechnology.ClientInterface.Client.Factories;
using PushTechnology.ClientInterface.Client.Features;
using PushTechnology.ClientInterface.Client.Features.Topics;
using PushTechnology.ClientInterface.Client.Session;
using PushTechnology.ClientInterface.Client.Topics.Details;
using PushTechnology.ClientInterface.Data.JSON;
using PushTechnology.ClientInterface.Logging;

namespace Examples {
    /// <summary>
    /// In this simple and commonest case for a client, we just subscribe to a few topics and assign handlers for each
    /// to receive content.
    ///
    /// This makes use of the <see cref="ITopics"/> feature only.
    ///
    /// To subscribe to a topic, the client session must have the <see cref="TopicPermission.READ_TOPIC"/> permission
    /// for that branch of the topic tree.
    /// </summary>
    public class ClientSimpleSubscriber {
        private readonly ISession session;
        private readonly static Logger LOG = new Logger();

        /// <summary>
        /// Constructor.
        /// </summary>
        public ClientSimpleSubscriber() {
            session = Diffusion.Sessions.Principal( "client" ).Password( "password" )
                .Open( "ws://diffusion.example.com:80" );

            // Use the Topics feature to add a topic stream for Foo and all topics under Bar and request subscription
            // to those topics
            var topics = session.GetTopicsFeature();

            topics.AddStream( ">Foo", new FooTopicStream() );
            topics.AddStream( ">Bar/", new BarTopicStream() );

            topics.Subscribe( Diffusion.TopicSelectors.AnyOf( "Foo", "Bar//" ), new TopicsCompletionCallbackDefault() );
        }

        /// <summary>
        /// Close the session.
        /// </summary>
        public void Close() {
            session.Close();
        }

        /// <summary>
        /// The topic stream for all messages on the 'Foo' topic.
        /// </summary>
        private class FooTopicStream : DefaultValueStream<IContent> {
            /// <summary>
            /// Topic update received.
            ///
            /// This indicates an update to the state of a topic that is subscribed to.
            /// </summary>
            /// <param name="topicPath">the full topic path.</param>
            /// <param name="content">the topic content. The context may contain more information about the nature
            /// of the content</param>
            /// <param name="context">the update context which may indicate whether the content represents the total
            /// state or a change to the state</param>
            public new void OnValue( string topicPath, ITopicSpecification specification, IContent oldValue,
                IContent newValue) {
                LOG.Info( newValue.AsString() );
            }
        }

        /// <summary>
        /// The topic stream for all messages on 'Bar' topics.
        /// </summary>
        private class BarTopicStream : DefaultValueStream<IJSON> {
            /// <summary>
            /// Topic update received.
            ///
            /// This indicates an update to the state of a topic that is subscribed
            /// to.
            /// </summary>
            /// <param name="topicPath">the full topic path.</param>
            /// <param name="content">the topic content. The context may contain more information about the nature
            /// of the content</param>
            /// <param name="context">the update context which may indicate whether the content represents the total
            /// state or a change to the state</param>
            public new void OnValue( string topicPath, ITopicSpecification specification, IJSON oldValue,
                IJSON newValue ) {
                LOG.Info( newValue.ToJSONString() );
            }
        }
    }
}