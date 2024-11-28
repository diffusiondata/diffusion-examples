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

import { connectShared } from 'diffusion';

export async function connectSharedExistingExample(): Promise<void> {
    /// tag::connection_establishment_connect_via_shared_worker[]
    // Connect to the server through an existing shared session.
    const session = await connectShared(
        'my-shared-session',
        'http://localhost/path/to/diffusion-worker.js'
    );

    console.log(`Connected. Session Identifier: ${session.sessionId.toString()}`);

    // Insert work here
    /// tag::log
    expect(session.sessionId).not.toBeNull();
    expect(session.isConnected()).toBe(true);
    /// end::log

    await session.closeSession();
    /// end::connection_establishment_connect_via_shared_worker[]
}
