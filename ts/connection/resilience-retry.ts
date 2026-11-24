/*******************************************************************************
 * Copyright (C) 2024 Diffusion Data Ltd.
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

export async function initialConnectRetryExample(): Promise<void> {

    /*
        A retry strategy provided within the connection options is employed when establishing
        a connection across the requested transport options.
     */
    let session: Session;
    try {
        // Connect to the server.
        session = await connect({
            host: 'localhost',
            port: 8080,
            principal: 'admin',
            credentials: 'password',
            // Optional transports property stated for clarity only (this is where retry logic will apply).
            transports: ['WS'],
            // Fixed RetryDescriptor based strategy (10 attempts, at fixed intervals)
            retry: {
                attempts: 10,
                interval: 250
            }
        });
    } catch (err) {
        console.error('Connection could not be established despite retries.', err);
        throw err;
    }
    console.log(`Connected. Session Identifier: ${session.sessionId.toString()}`);

    /*
        An alternative form of retry strategy, allowing more control
     */
    let session2: Session;
    try {
        // Connect to the server.
        session2 = await connect({
            host: 'localhost',
            port: 8080,
            principal: 'admin',
            credentials: 'password',
            // Optional transports property stated for clarity only (this is where retry logic will apply).
            transports: ['WS'],
            // Dynamic RetryController (10 attempts, increasing intervals)
            retry: (retryAttempts: number) => {
                if (retryAttempts > 9) {
                    return null;
                }
                return (retryAttempts + 1) * 250;
            }
        });
    } catch (err) {
        console.error('Connection could not be established despite retries.', err);
        throw err;
    }
    console.log(`Connected. Session Identifier: ${session2.sessionId.toString()}`);

    // Insert work here

    await session.closeSession();
    await session2.closeSession();
}
