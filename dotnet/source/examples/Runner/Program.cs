/**
 * Copyright © 2016, 2021 Push Technology Ltd.
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

                /// Start AddAndSet example
                //runner.Start( new AddAndSetTopic(), secureUrl );

                /// Start time series add topics example
                //runner.Start(new PublishingTimeSeriesAddTopics(), secureUrl);
                //runner.Start(new ConsumingTimeSeriesTopics(), url);

                /// Start time series topic append example
                //runner.Start(new PublishingTimeSeriesAppend(), secureUrl);
                //runner.Start(new ConsumingTimeSeriesTopics(), url);

                /// Start time series topic append with timestamp example
                //runner.Start(new PublishingTimeSeriesAppendWithTimestamp(), secureUrl);
                //runner.Start(new ConsumingTimeSeriesTopics(), url);

                /// Start time series topic edit example
                //runner.Start(new PublishingTimeSeriesEdit(), secureUrl);
                //runner.Start(new ConsumingTimeSeriesTopics(), url);

                /// Start time series range query example
                //runner.Start(new PublishingTimeSeriesRangeQuery(), secureUrl);
                //runner.Start(new ConsumingTimeSeriesTopics(), url);

                /// Start create topic view example
                //runner.Start( new TopicViewsCreate(), secureUrl );

                /// Start list topic view example
                //runner.Start( new TopicViewsList(), secureUrl);

                /// Start list topic view example
                //runner.Start( new TopicViewsRemove(), secureUrl);

                /// Start create update stream example
                //runner.Start(new UpdateStreamCreate(), secureUrl);

                /// Start create update stream with constraint example
                //runner.Start(new UpdateStreamCreateWithConstraint(), secureUrl);

                /// Start remove topics example
                //runner.Start(new RemoveTopics(), secureUrl);

                /// Start set topic with constraint example
                //runner.Start(new SetTopicWithConstraint(), secureUrl);

                /// Start add and set topic with constraint example
                //runner.Start(new AddAndSetTopicWithConstraint(), secureUrl);

                /// Start apply JSON patch example
                //runner.Start(new ApplyJSONPatch(), secureUrl);

                /// Start Control Authentication Client example
                //runner.Start(new ControlClientChangingSystemAuthentication(), secureUrl);

                /// Start add fallback stream example
                //runner.Start(new AddFallbackStream(), secureUrl);

                /// Start add missing topic handler example
                //runner.Start(new AddMissingTopicHandler(), secureUrl);

                /// Start topic notification listener example
                //runner.Start(new TopicNotificationListener(), secureUrl);

                /// Start client control change roles example
                //runner.Start(new ClientControlChangeRoles(), secureUrl);

                /// Start client control change roles filter example
                //runner.Start(new ClientControlChangeRolesFilter(), secureUrl);

                /// Start client control close session example
                //runner.Start(new ClientControlCloseSession(), secureUrl);

                /// Start client control close session filter example
                //runner.Start(new ClientControlCloseSessionFilter(), secureUrl);

                /// Start client control get session properties example
                //runner.Start(new ClientControlGetSessionProperties(), secureUrl);

                /// Start client control set conflated example
                //runner.Start(new ClientControlSetConflated(), secureUrl);

                /// Start client control set conflated filter example
                //runner.Start(new ClientControlSetConflatedFilter(), secureUrl);

                /// Start client control set session properties example
                //runner.Start(new ClientControlSetSessionProperties(), secureUrl);

                /// Start client control set session properties filter example
                //runner.Start(new ClientControlSetSessionPropertiesFilter(), secureUrl);

                /// Start client control set session properties listener example
                //runner.Start(new ClientControlSetSessionPropertiesListener(), secureUrl);

                /// Start subscription control routing handler example
                //runner.Start(new SubscriptionControlRoutingHandler(), secureUrl);

                /// Start subscription control subscribe example
                //runner.Start(new SubscriptionControlSubscribe(), secureUrl);

                /// Start subscription control subscribe with a filter example
                //runner.Start(new SubscriptionControlSubscribeFilter(), secureUrl);

                /// Start Remote Servers example
                //runner.Start(new RemoteServers(), secureUrl);
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
