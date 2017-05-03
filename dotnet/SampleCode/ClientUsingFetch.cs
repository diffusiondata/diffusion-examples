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

using PushTechnology.ClientInterface.Client.Factories;
using PushTechnology.ClientInterface.Client.Features;
using PushTechnology.ClientInterface.Client.Session;

namespace Examples {
    /// <summary>
    /// This is a simple example of a client that fetches the state of topics but does not subscribe to them.
    ///
    /// This makes use of the <see cref="ITopics"/> feature only.
    /// </summary>
    public class ClientUsingFetch {
        private readonly ISession session;
        private readonly ITopics topics;

        public ClientUsingFetch() {
            session = Diffusion.Sessions.Principal( "client" ).Password( "password" )
                .Open( "ws://diffusion.example.com:80" );

            topics = session.GetTopicsFeature();
        }

        /// <summary>
        /// Issues a fetch request for a topic or selection of topics.
        /// </summary>
        /// <param name="topicSelector">A <see cref="TopicSelector"/> expression.</param>
        /// <param name="fetchContext">The context string to be returned with the fetch response(s).</param>
        /// <param name="stream">The callback for fetch responses.</param>
        public void Fetch( string topicSelector, string fetchContext, IFetchContextStream<string> stream ) {
            topics.Fetch( topicSelector, fetchContext, stream );
        }

        /// <summary>
        /// Close the session.
        /// </summary>
        public void Close() {
            session.Close();
        }
    }
}