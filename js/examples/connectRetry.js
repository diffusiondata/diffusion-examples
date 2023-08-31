/*******************************************************************************
 * Copyright (C) 2022 - 2023 DiffusionData Ltd.
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
 *******************************************************************************/

import { connect, Session } from 'diffusion';

// example showcasing how to set an initial connection retry
export async function connectionRetryExample() {

    // When establishing a session, it is possible to specify whether the initial
    // connection attempt should be retried when a failure occurs

    // Connect to the server.
    const session = await connect({
        host: 'diffusion.example.com',
        port: 443,
        secure: true,
        principal: 'control',
        credentials: 'password',
        retry: {
            // perform 5 initial connection attempts before giving up
            attempts: 5,
            // set 300 ms between connection attempts
            interval: 300
        }
    });

    console.log(`Connection established ${session.sessionId}`);
}
