/**
 * Copyright © 2024 Diffusion Data Ltd.
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
using PushTechnology.ClientInterface.Client.Factories;
using PushTechnology.ClientInterface.Client.Session;
using static System.Console;
using static PushTechnology.ClientInterface.Examples.Program;

namespace PushTechnology.ClientInterface.Examples.ServerConfiguration.SystemAuthenticationControl
{
    public sealed class VerifyPrincipalPassword : Example
    {
        public override async Task Run(CancellationToken cancellationToken, string[] args)
        {
            string serverUrl = args[0];

            var session1 = Diffusion.Sessions.Principal("admin").Password("password")
                .Open(serverUrl);

            WriteLine($"Verify principal password.");

            string updateScript = session1.SystemAuthenticationControl.Script
                .VerifyPassword("control", "password")
                .SetPassword("control", "12345")
                .ToScript();

            await session1.SystemAuthenticationControl.UpdateStoreAsync(updateScript, cancellationToken);

            var session2 = Diffusion.Sessions.Principal("control").Password("12345")
                .Open(serverUrl);

            session2.Close();

            try
            {
                updateScript = session1.SystemAuthenticationControl.Script
                    .VerifyPassword("control", "this_is_not_the_right_password")
                    .SetPassword("control", "new_password")
                    .ToScript();


                await session1.SystemAuthenticationControl.UpdateStoreAsync(updateScript, cancellationToken);
            }
            catch (Exception ex)
            {
                WriteLine(ex.Message);
            }

            try
            {
                var session3 = Diffusion.Sessions.Principal("control").Password("new_password")
                    .Open(serverUrl);
            }
            catch (Exception ex)
            {
                WriteLine(ex.Message);
            }

            session1.Close();
        }
    }
}
