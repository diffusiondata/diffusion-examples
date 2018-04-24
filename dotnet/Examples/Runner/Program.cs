/**
 * Copyright © 2016, 2017 Push Technology Ltd.
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

using PushTechnology.ClientInterface.Examples.Client;
using PushTechnology.ClientInterface.Examples.Control;

namespace PushTechnology.ClientInterface.Examples.Runner {

    /// <summary>
    /// This is used to run the examples.
    /// </summary>
    public static class Program {
        /// <summary>
        /// To run an example set, uncomment the corresponding block of code.
        /// </summary>
        /// <param name="args">The program arguments.</param>
        public static void Main( string[] args ) {
            var url = "ws://localhost:8080";
            var secureUrl = "wss://localhost:8080";

            using ( var runner = new ExampleRunner() ) {
                // Start JSON Topic Examples
                // runner.Start( new UpdatingJSONTopics(), secureUrl );
                // runner.Start( new ConsumingJSONTopics(), url );

                // Start Record Topic Examples
                // runner.Start( new UpdatingRecordTopics(), secureUrl );
                // runner.Start( new ConsumingRecordTopics(), url );

                // Start ping examples
                // runner.Start( new PingServer(), url );
            }
        }
    }
}