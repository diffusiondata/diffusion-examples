/**
 * Copyright © 2021 Push Technology Ltd.
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
using PushTechnology.ClientInterface.Client.Callbacks;
using PushTechnology.ClientInterface.Client.Factories;
using PushTechnology.ClientInterface.Client.Features;
using PushTechnology.ClientInterface.Client.Features.Topics;
using PushTechnology.ClientInterface.Client.Session;
using PushTechnology.ClientInterface.Client.Topics;
using PushTechnology.ClientInterface.Client.Topics.Details;
using static System.Console;
using static PushTechnology.ClientInterface.Examples.Runner.Program;

namespace PushTechnology.ClientInterface.Example.Features
{
    /// <summary>
    /// Client implementation that demonstrates session locks.
    /// </summary>
    public sealed class SessionLocks : IExample
    {
        private ISession session1, session2;
        private ISessionLock sessionLock1, sessionLock2;

        private static string LOCK_NAME = "lockA";

        /// <summary>
        /// Runs the session locks example.
        /// </summary>
        /// <param name="cancellationToken">A token used to end the client example.</param>
        /// <param name="args">A single string should be used for the server url.</param>
        public async Task Run(CancellationToken cancellationToken, string[] args)
        {
            string serverUrl = args[0];
            session1 = Diffusion.Sessions.Principal("control").Password("password")
                .CertificateValidation((cert, chain, errors) => CertificateValidationResult.ACCEPT)
                .Open(serverUrl);

            session2 = Diffusion.Sessions.Principal("control").Password("password")
                .CertificateValidation((cert, chain, errors) => CertificateValidationResult.ACCEPT)
                .Open(serverUrl);

            WriteLine("Sessions 1 and 2 have been created.");

            AcquireLockSession1();
        }

        private async void AcquireLockSession1()
        {
            try
            {
                WriteLine("Requesting lock 1...");

                sessionLock1 = await session1.LockAsync(LOCK_NAME, SessionLockScope.UNLOCK_ON_CONNECTION_LOSS);

                WriteLine("Lock 1 has been acquired.");

                AcquireLockSession2();

                Thread.Sleep(1000);

                ReleaseLock1();
            }
            catch (Exception ex)
            {
                WriteLine($"Failed to get lock 1 : {ex}.");
                session1.Close();
                session2.Close();
            }
        }

        private async void AcquireLockSession2()
        {
            try
            {
                WriteLine("Requesting lock 2...");

                sessionLock2 = await session2.LockAsync(LOCK_NAME, SessionLockScope.UNLOCK_ON_CONNECTION_LOSS);

                WriteLine("Lock 2 has been acquired.");

                Thread.Sleep(1000);

                ReleaseLock2();
            }
            catch (Exception ex)
            {
                WriteLine($"Failed to get lock 2 : {ex}.");
                session1.Close();
                session2.Close();
            }
        }

        private async void ReleaseLock1()
        {
            try
            {
                WriteLine("Requesting lock 1 release...");

                await sessionLock1.UnlockAsync();

                WriteLine("Lock 1 has been released.");
            }
            catch (Exception ex)
            {
                WriteLine($"Failed to release lock 1 : {ex}.");
                session1.Close();
                session2.Close();
            }
        }

        private async void ReleaseLock2()
        {
            try
            {
                WriteLine("Requesting lock 2 release...");

                await sessionLock2.UnlockAsync();

                WriteLine("Lock 2 has been released.");
            }
            catch (Exception ex)
            {
                WriteLine($"Failed to release lock 2 : {ex}.");
            }
            finally
            {
                session1.Close();
                session2.Close();
            }
        }
    }
}