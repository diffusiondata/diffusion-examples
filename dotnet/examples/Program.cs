/**
* Copyright © 2023 - 2024 Diffusion Data Ltd.
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
using System.Threading.Tasks;
using System.Threading;

using PushTechnology.ClientInterface.Examples.Connection.Establishment;
using PushTechnology.ClientInterface.Examples.Connection.Resilience;
using PushTechnology.ClientInterface.Examples.Wrangling.TopicViews.API;
using PushTechnology.ClientInterface.Examples.Wrangling.SessionTrees;
using PushTechnology.ClientInterface.Examples.Messaging;
using PushTechnology.ClientInterface.Examples.Monitoring;
using PushTechnology.ClientInterface.Examples.Ping;
using PushTechnology.ClientInterface.Examples.PubSub.FetchTopics;
using PushTechnology.ClientInterface.Examples.PubSub.PublishingTopics;
using PushTechnology.ClientInterface.Examples.PubSub.PublishingTopicsWithConstraint;
using PushTechnology.ClientInterface.Examples.PubSub.RemovingTopics;
using PushTechnology.ClientInterface.Examples.PubSub.SubscribingToTopics;
using PushTechnology.ClientInterface.Examples.TimeSeries;
using PushTechnology.ClientInterface.Examples.Security;
using PushTechnology.ClientInterface.Examples.ServerConfiguration.Metrics.SessionMetricCollector;
using PushTechnology.ClientInterface.Examples.ServerConfiguration.Metrics.TopicMetricCollector;
using PushTechnology.ClientInterface.Examples.ServerConfiguration.RemoteServers;
using PushTechnology.ClientInterface.Examples.ServerConfiguration.SystemAuthenticationControl;
using PushTechnology.ClientInterface.Examples.ServerConfiguration.SecurityControl;
using PushTechnology.ClientInterface.Examples.SessionManagement;
using PushTechnology.ClientInterface.Examples.SessionManagement.ClientControl;
using PushTechnology.ClientInterface.Examples.Wrangling.TopicViews.DSL;

namespace PushTechnology.ClientInterface.Examples
{
    /// <summary>
    /// This is used to run the examples.
    /// </summary>
    public static class Program
    {

        /// <summary>
        /// To run an example, uncomment the corresponding block of code.
        /// </summary>
        /// <param name="args">The program arguments.</param>
        public static void Main(string[] args)
        {
            var url = "ws://localhost:8080";
            var secureUrl = "wss://localhost:8080";

            using (var runner = new ExampleRunner())
            {
                // Start the connect synchronously example
                //runner.Start(new ConnectSynchronously(), url);

                // Start the connect using a session factory example
                //runner.Start(new ConnectUsingASessionFactory(), url);

                // Start the connect securely example
                //runner.Start(new ConnectSecurelyAcceptingAllServerCertificates(), secureUrl);

                // Start the session state listener example
                //runner.Start(new SessionStateListener(), url);

                // Start the reconnection strategy example
                //runner.Start(new ReconnectionStrategy(), url);

                // Start the initial session establishment retry mechanism example
                //runner.Start(new InitialSessionEstablishmentRetryMechanism(), url);

                // Start the ping server example
                //runner.Start(new PingServer(), url);

                // Start the change principal example
                //runner.Start(new ChangePrincipal(), url);

                // Start the get global permissions example
                //runner.Start(new GetGlobalPermissions(), url);

                // Start the get path permissions example
                //runner.Start(new GetPathPermissions(), url);

                // Start the add topic example
                //runner.Start(new AddTopic(), url);

                // Start the add topic with custom topic properties example
                //runner.Start(new AddTopicWithCustomTopicProperties(), url);

                // Start the add and set topic example
                //runner.Start(new AddAndSetTopic(), url);

                // Start the set topic example
                //runner.Start(new SetTopic(), url);

                // Start the add and set topic using update stream example
                //runner.Start(new AddAndSetTopicUsingUpdateStream(), url);

                // Start the set topic using update stream example
                //runner.Start(new SetTopicUsingUpdateStream(), url);

                // Start the add and set topic with no topic constraint example
                //runner.Start(new AddAndSetTopicNoTopic(), url);

                // Start the add and set topic with session lock constraint example
                //runner.Start(new AddAndSetTopicSessionLock(), url);

                // Start the add and set topic with value constraint example
                //runner.Start(new AddAndSetTopicValue(), url);

                // Start the add and set topic with JSON value with constraint example
                //runner.Start(new AddAndSetTopicJSONValueWith(), url);

                // Start the add and set topic with JSON value without constraint example
                //runner.Start(new AddAndSetTopicJSONValueWithout(), url);

                // Start the set topic with no value constraint example
                //runner.Start(new SetTopicNoValue(), url);

                // Start the set topic with session lock constraint example
                //runner.Start(new SetTopicSessionLock(), url);

                // Start the set topic with value constraint example
                //runner.Start(new SetTopicValue(), url);

                // Start the set topic with JSON value with constraint example
                //runner.Start(new SetTopicJSONValueWith(), url);

                // Start the set topic with JSON value without constraint example
                //runner.Start(new SetTopicJSONValueWithout(), url);

                // Start the add and set topic with and constraint example
                //runner.Start(new AddAndSetTopicAnd(), url);

                // Start the subscribe to single topic using topic path example
                //runner.Start(new SubscribeToSingleTopicUsingTopicPath(), url);

                // Start the subscribe to multiple topics using topic selector example
                //runner.Start(new SubscribeToMultipleTopicsUsingTopicSelector(), url);

                // Start the subscribe using fallback streams example
                //runner.Start(new SubscribeUsingFallbackStreams(), url);

                // Start the subscribe with cross compatible value stream example
                //runner.Start(new SubscribeWithCrossCompatibleValueStream(), url);

                // Start the removing a single topic using topic path example
                //runner.Start(new RemovingASingleTopicUsingTopicPath(), url);

                // Start the removing multiple topics using a topic selector example
                //runner.Start(new RemovingMultipleTopicsUsingATopicSelector(), url);

                // Start the removing topics with automatic topic removal example
                //runner.Start(new RemovingTopicsWithAutomaticTopicRemoval(), url);

                // Start the fetch topic properties example
                //runner.Start(new FetchTopicProperties(), url);

                // Start the fetch multiple topics by iterating through paging example
                //runner.Start(new FetchMultipleTopicsByIteratingThroughPaging(), url);

                // Start the json patch add example
                //runner.Start(new JSONPatchAdd(), url);

                // Start the json patch copy example
                //runner.Start(new JSONPatchCopy(), url);

                // Start the json patch move example
                //runner.Start(new JSONPatchMove(), url);

                // Start the json patch remove example
                //runner.Start(new JSONPatchRemove(), url);

                // Start the json patch replace example
                //runner.Start(new JSONPatchReplace(), url);

                // Start the json patch test example
                //runner.Start(new JSONPatchTest(), url);

                // Start the create timeSeries topic example
                //runner.Start(new CreateTimeSeriesTopic(), url);

                // Start the append to timeSeries topic example
                //runner.Start(new AppendToTimeSeriesTopic(), url);

                // Start the subscribe to timeSeries topics example
                //runner.Start(new SubscribeToTimeSeriesTopics(), url);

                // Start the append to timeSeries topic with user supplied timestamp example
                //runner.Start(new AppendToTimeSeriesTopicWithUserSuppliedTimestamp(), url);

                // Start the append to timeSeries topic via update stream example
                //runner.Start(new AppendToTimeSeriesTopicViaUpdateStream(), url);

                // Start the edit timeSeries topic example
                //runner.Start(new EditTimeSeriesTopic(), url);

                // Start the range query a timeSeries topic example
                //runner.Start(new RangeQueryATimeSeriesTopic(), url);

                // Start the timeSeries cross compatible datatypes example
                //runner.Start(new TimeSeriesCrossCompatibleDatatypes(), url);

                // Start the message to messagePath example
                //runner.Start(new MessageToMessagePath(), url);

                // Start the message to session ID example
                //runner.Start(new MessageToSessionID(), url);

                // Start the message to session filter example
                //runner.Start(new MessageToSessionFilter(), url);

                // Start the add topic view example
                //runner.Start(new AddTopicView(), url);

                // Start the list topic views example
                //runner.Start(new ListTopicViews(), url);

                // Start the remove topic views example
                //runner.Start(new RemoveTopicViews(), url);

                // Start the source path directive example
                //runner.Start(new SourcePathDirective(), url);

                // Start the remote topic views example
                //runner.Start(new RemoteTopicViews(), url);

                // Start the scalar directive example
                //runner.Start(new ScalarDirective(), url);

                // Start the expand value example
                //runner.Start(new ExpandValue(), url);

                // Start the process transformations set example
                //runner.Start(new ProcessTransformationsSet(), url);

                // Start the process transformations remove example
                //runner.Start(new ProcessTransformationsRemove(), url);

                // Start the process transformations continue example
                //runner.Start(new ProcessTransformationsContinue(), url);

                // Start the patch transformations add example
                //runner.Start(new PatchTransformationsAdd(), url);

                // Start the patch transformations remove example
                //runner.Start(new PatchTransformationsRemove(), url);

                // Start the patch transformations replace example
                //runner.Start(new PatchTransformationsReplace(), url);

                // Start the patch transformations move example
                //runner.Start(new PatchTransformationsMove(), url);

                // Start the patch transformations copy example
                //runner.Start(new PatchTransformationsCopy(), url);

                // Start the patch transformations test example
                //runner.Start(new PatchTransformationsTest(), url);

                // Start the insert transformations example
                //runner.Start(new InsertTransformations(), url);

                // Start the options topic property mapping example
                //runner.Start(new OptionsTopicPropertyMapping(), url);

                // Start the options topic value example
                //runner.Start(new OptionsTopicValue(), url);

                // Start the options throttle example
                //runner.Start(new OptionsThrottle(), url);

                // Start the options delay example
                //runner.Start(new OptionsDelay(), url);

                // Start the options separator example
                //runner.Start(new OptionsSeparator(), url);

                // Start the options preserve topics example
                //runner.Start(new OptionsPreserveTopics(), url);

                // Start the options topic type example
                //runner.Start(new OptionsTopicType(), url);

                // Start the put branch mapping table example
                //runner.Start(new PutBranchMappingTable(), url);

                // Start the list session tree branches with mappings example
                //runner.Start(new ListSessionTreeBranchesWithMappings(), url);

                // Start the get branch mapping table example
                //runner.Start(new GetBranchMappingTable(), url);

                // Start the session trees use case example
                //runner.Start(new UseCase(), url);

                // Start the put and remove branch mapping table example
                //runner.Start(new PutAndRemoveBranchMappingTable(), url);

                // Start the missing topic notifications example
                //runner.Start(new MissingTopicNotifications(), url);

                // Start the topic notifications example
                //runner.Start(new TopicNotifications(), url);

                // Start the session event listener example
                //runner.Start(new SessionEventListener(), url);

                // Start the subscription control example
                //runner.Start(new SubscriptionControl(), url);

                // Start the authentication control example
                //runner.Start(new AuthenticationControl(), url);

                // Start the get session properties via session ID example
                //runner.Start(new GetSessionPropertiesViaSessionID(), url);

                // Start the set session properties via session ID example
                //runner.Start(new SetSessionPropertiesViaSessionID(), url);

                // Start the set session properties via session filter example
                //runner.Start(new SetSessionPropertiesViaSessionFilter(), url);

                // Start the close client via session ID example
                //runner.Start(new CloseClientViaSessionID(), url);

                // Start the close client via session filter example
                //runner.Start(new CloseClientViaSessionFilter(), url);

                // Start the change roles via session ID example
                //runner.Start(new ChangeRolesViaSessionID(), url);

                // Start the change roles via session filter example
                //runner.Start(new ChangeRolesViaSessionFilter(), url);

                // Start the control client queue conflation via session ID example
                //runner.Start(new ControlClientQueueConflationViaSessionID(), url);

                // Start the control client queue conflation via session filter example
                //runner.Start(new ControlClientQueueConflationViaSessionFilter(), url);

                // Start the put session metric collector example
                //runner.Start(new PutSessionMetricCollector(), url);

                // Start the list session metric collectors example
                //runner.Start(new ListSessionMetricCollectors(), url);

                // Start the remove session metric collector example
                //runner.Start(new RemoveSessionMetricCollector(), url);

                // Start the put topic metric collector example
                //runner.Start(new PutTopicMetricCollector(), url);

                // Start the list topic metric collectors example
                //runner.Start(new ListTopicMetricCollectors(), url);

                // Start the remove topic metric collector example
                //runner.Start(new RemoveTopicMetricCollector(), url);

                // Start the get outbound bytes example
                //runner.Start(new GetOutboundBytes(), url);

                // Start the get metrics console example
                //runner.Start(new GetMetricsConsole(), url);

                // Start the isolate path example
                //runner.Start(new IsolatePath(), url);

                // Start the deisolate path example
                //runner.Start(new DeisolatePath(), url);

                // Start the set path permissions example
                //runner.Start(new SetPathPermissions(), url);

                // Start the remove path permissions example
                //runner.Start(new RemovePathPermissions(), url);

                // Start the set default path permissions example
                //runner.Start(new SetDefaultPathPermissions(), url);

                // Start the set global permissions example
                //runner.Start(new SetGlobalPermissions(), url);

                // Start the define roles hierarchy example
                //runner.Start(new DefineRolesHierarchy(), url);

                // Start the restrict role edit permissions example
                //runner.Start(new RestrictRoleEditPermissions(), url);

                // Start the set default roles for anonymous sessions example
                //runner.Start(new SetDefaultRolesForAnonymousSessions(), url);

                // Start the set default roles for named sessions example
                //runner.Start(new SetDefaultRolesForNamedSessions(), url);

                // Start the deny anonymous connections example
                //runner.Start(new DenyAnonymousConnections(), url);

                // Start the abstain anonymous connections example
                //runner.Start(new AbstainAnonymousConnections(), url);

                // Start the allow anonymous connections example
                //runner.Start(new AllowAnonymousConnections(), url);

                // Start the add principal example
                //runner.Start(new AddPrincipal(), url);

                // Start the add locked principal example
                //runner.Start(new AddLockedPrincipal(), url);

                // Start the remove principal example
                //runner.Start(new RemovePrincipal(), url);

                // Start the assign roles example
                //runner.Start(new AssignRoles(), url);

                // Start the change principal's password example
                //runner.Start(new ChangePrincipalPassword(), url);

                // Start the verify principal's password example
                //runner.Start(new VerifyPrincipalPassword(), url);

                // Start the trust client proposed property value from allowed value example
                //runner.Start(new TrustClientProposedPropertyIn(), url);

                // Start the trust client proposed property value that matches regular expression example
                //runner.Start(new TrustClientProposedPropertyMatches(), url);

                // Start the ignore client proposed property example
                //runner.Start(new IgnoreClientProposedProperty(), url);

                // Start the create primary initiator remote server example
                //runner.Start(new CreatePrimaryInitiatorRemoteServer(), url);

                // Start the create secondary initiator remote server example
                //runner.Start(new CreateSecondaryInitiatorRemoteServer(), url);

                // Start the create secondary acceptor remote server example
                //runner.Start(new CreateSecondaryAcceptorRemoteServer(), url);

                // Start the list remote servers example
                //runner.Start(new ListRemoteServers(), url);

                // Start the check remote servers example
                //runner.Start(new CheckRemoteServers(), url);

                // Start the remove remote servers example
                //runner.Start(new RemoveRemoteServers(), url);
            }
        }

        /// <summary>
        /// Interface to be used by all examples.
        /// </summary>
        public interface IExample
        {
            /// <summary>
            /// Runs the current example.
            /// </summary>
            /// <remarks>
            /// This acts as the main method for examples.
            /// </remarks>
            /// <param name="cancel">The cancellation token to cancel the current example run.</param>
            /// <param name="args">The optional example arguments.</param>
            Task Run(CancellationToken cancel, string[] args);

            Task RunWrapper(CancellationToken cancel, string[] args);
            AutoResetEvent CompletedEvent { get; set; }
        }

        public abstract class Example : IExample
        {

            public virtual AutoResetEvent CompletedEvent { get; set; }

            public async Task RunWrapper(CancellationToken cancel, string[] args)
            {
                await Run(cancel, args).ConfigureAwait(false);
                CompletedEvent.Set();
            }

            public abstract Task Run(CancellationToken cancel, string[] args);

            public Example() => CompletedEvent = new AutoResetEvent(false);
        }

        /// <summary>
        /// Class used by the Main method in <see cref="Program"/> to start a new cancelable task
        /// for each <see cref="IExample"/> implementation.
        /// </summary>
        private sealed class ExampleRunner : IDisposable
        {
            private readonly List<Task> runningExamples = new List<Task>();
            private readonly CancellationTokenSource cancellationTokenSource = new CancellationTokenSource();

            /// <summary>
            /// Starts a new task for an <see cref="IExample"/> implementation.
            /// </summary>
            /// <param name="example">The example to run.</param>
            /// <param name="args">An array of arguments. Depending on what is necessary for an example,
            /// it may contain multiple variables, such as serverUrl, topic paths etc. Check the example class
            /// for the description of what is required for this array.</param>
            public void Start(IExample example, params string[] args)
            {
                var task = Task.Run(async () => {
                    var run = example?.RunWrapper(cancellationTokenSource.Token, args);
                    if (run != null)
                    {
                        await run;
                    }
                });
                runningExamples.Add(task);

                example.CompletedEvent.WaitOne(50000);
            }

            /// <summary>
            /// Method used to wait for examples to be canceled by the user.
            /// </summary>
            public void Dispose()
            {
                cancellationTokenSource.Cancel();
                Task.WaitAll(runningExamples.ToArray());
            }
        }
    }
}

