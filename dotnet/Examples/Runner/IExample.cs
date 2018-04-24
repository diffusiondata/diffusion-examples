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

using System.Threading;
using System.Threading.Tasks;

namespace PushTechnology.ClientInterface.Examples.Runner {
    /// <summary>
    /// Interface to be used by all examples.
    /// </summary>
    public interface IExample {
        /// <summary>
        /// Runs the curent example.
        /// </summary>
        /// <remarks>
        /// This acts as the main method for examples.
        /// </remarks>
        /// <param name="cancel">The cancellation token to cancel the current example run.</param>
        /// <param name="args">The optional example arguments.</param>
        Task Run( CancellationToken cancel, string[] args );
    }
}