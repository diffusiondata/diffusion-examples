/**
 * Copyright © 2016, 2018 Push Technology Ltd.
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
using System.Threading;
using System.Threading.Tasks;
using PushTechnology.ClientInterface.Example.Consuming;
using PushTechnology.ClientInterface.Example.Features;
using PushTechnology.ClientInterface.Example.Publishing;

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
                /// Start JSON topic examples
                //runner.Start( new PublishingJSONTopics(), secureUrl );
                //runner.Start( new ConsumingJSONTopics(), url );

                /// Start string topic examples
                //runner.Start( new PublishingStringTopics(), secureUrl );
                //runner.Start( new ConsumingStringTopics(), url );

                /// Start integer topic examples
                //runner.Start( new PublishingIntegerTopics(), secureUrl );                
                //runner.Start( new ConsumingIntegerTopics(), url );

                /// Start double topic examples
                //runner.Start( new PublishingDoubleTopics(), secureUrl );
                //runner.Start( new ConsumingDoubleTopics(), url );

                /// Start binary topic examples
                //runner.Start( new PublishingBinaryTopics(), secureUrl );
                //runner.Start( new ConsumingBinaryTopics(), url );

                /// Start RecordV2 topic examples
                //runner.Start( new PublishingRecordV2Topics(), secureUrl );
                //runner.Start( new ConsumingRecordV2Topics(), url );

                /// Start messaging examples
                //runner.Start( new ReceivingMessages(), secureUrl );
                //runner.Start( new SendingMessages(), secureUrl );

                /// Start Request/Response to path examples
                //runner.Start( new ReceivingPathRequestMessages(), secureUrl );
                //runner.Start( new SendingPathRequestMessages(), secureUrl );

                /// Start Request/Response to session filter examples
                //runner.Start( new ReceivingFilterRequestMessages(), secureUrl );
                //runner.Start( new SendingFilterRequestMessages(), secureUrl );

                /// Start Request/Response to specified session examples
                //runner.Start( new ReceivingSessionRequestMessages(), secureUrl );
                //runner.Start( new SendingSessionRequestMessages(), secureUrl );

                /// Start ping example
                //runner.Start( new Example.Features.PingServer(), secureUrl );

                /// Start automatic topic removal
                //runner.Start( new Removal(), secureUrl );

                /// Start authentication example
                //runner.Start( new Authentication(), secureUrl );
            }
        }

        /// <summary>
        /// Interface to be used by all examples.
        /// </summary>
        public interface IExample {
            /// <summary>
            /// Runs the current example.
            /// </summary>
            /// <remarks>
            /// This acts as the main method for examples.
            /// </remarks>
            /// <param name="cancel">The cancellation token to cancel the current example run.</param>
            /// <param name="args">The optional example arguments.</param>
            Task Run( CancellationToken cancel, string[] args );
        }

        /// <summary>
        /// Class used by the Main method in <see cref="Program"/> to start a new cancelable task
        /// for each <see cref="IExample"/> implementation.
        /// </summary>
        private sealed class ExampleRunner : IDisposable {
            private readonly List<Task> runningExamples = new List<Task>();
            private readonly CancellationTokenSource cancellationTokenSource = new CancellationTokenSource();

            /// <summary>
            /// Starts a new task for an <see cref="IExample"/> implementation.
            /// </summary>
            /// <param name="example">The example to run.</param>
            /// <param name="args">An array of arguments. Depending on what is necessary for an example,
            /// it may contain multiple variables, such as serverUrl, topic paths etc. Check the example class
            /// for the description of what is required for this array.</param>
            public void Start( IExample example, params string[] args ) {
                var task = Task.Run( async () => {
                    var run = example?.Run( cancellationTokenSource.Token, args );
                    if ( run != null ) {
                        await run;
                    }
                } );
                runningExamples.Add( task );
            }

            /// <summary>
            /// Method used to wait for examples to be canceled by the user.
            /// </summary>
            /// <remarks>
            /// Pressing any key will stop the examples.
            /// </remarks>
            public void Dispose() {
                // Wait for key press to cancel
                Console.ReadKey( true );
                cancellationTokenSource.Cancel();
                Task.WaitAll( runningExamples.ToArray() );
            }
        }
    }
}
