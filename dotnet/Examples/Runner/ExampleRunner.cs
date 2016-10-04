/**
 * Copyright © 2016 Push Technology Ltd.
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

namespace PushTechnology.ClientInterface.Examples.Runner {
    /// <summary>
    /// Class used by the Main method in <see cref="Program"/> to start a new cancelalble task
    /// for each <see cref="IExample"/> implementation.
    /// </summary>
    public sealed class ExampleRunner {
        private readonly List<Task> runningExamples =
            new List<Task>();
        private readonly CancellationTokenSource cancellationTokenSource =
            new CancellationTokenSource();

        /// <summary>
        /// Starts a new task for an <see cref="IExample"/> implemetation.
        /// </summary>
        /// <param name="example">The example to run.</param>
        /// <param name="args">An array of arguments. Depending on what is necesarry for an example,
        /// it may contain multiple variables, such as serverUrl, topic paths etc. Check the example class
        /// for the description of what is required for this array.</param>
        public void StartExample( IExample example, params string[] args ) {
            var task = Task.Run( () => example.Run( cancellationTokenSource.Token, args ) );
            runningExamples.Add( task );
        }

        /// <summary>
        /// Method used to wait for examples to be canceled by the user.
        /// </summary>
        /// <remarks>
        /// Pressing any key will stop the examples.
        /// </remarks>
        public void AwaitCancellation() {
            // Wait for key press to cancel
            Console.ReadKey( true );
            cancellationTokenSource.Cancel();
            Task.WaitAll( runningExamples.ToArray() );
        }

    }
}
