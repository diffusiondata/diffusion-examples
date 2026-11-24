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

import { CloseReason, connect } from 'diffusion';

export async function sessionStateListenerExample(): Promise<void> {
    // Connect to the server.
    const session = await connect({
        host: 'localhost',
        port: 8080,
        principal: 'admin',
        credentials: 'password'
    });

    // Attach state listeners
    session.on({
        disconnect : () => {
            console.log('State changed to disconnected');
        },
        reconnect : () => {
            console.log('State changed to reconnected');
        },
        error : (error: Error) => {
            console.log('An error occured', error);
        },
        close : (reason: CloseReason) => {
            console.log('State changed to closed', reason);
        }
    });

    // Insert work here

    await session.closeSession();
}
