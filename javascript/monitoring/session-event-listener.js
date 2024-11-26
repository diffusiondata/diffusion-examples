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

const diffusion = require('diffusion');

export async function monitoringSessionEventListener() {
    const session1 = await diffusion.connect({
        host: 'localhost',
        port: 8080,
        principal: 'admin',
        credentials: 'password'
    });

    const session2 = await diffusion.connect({
        host: 'localhost',
        port: 8080,
        principal: 'client',
        credentials: 'password'
    });

    const sessionEventStream = {
        onSessionEvent: (event) => {
            if (event.isOpenEvent()) {
                console.log(`New session: id=${event.sessionId}`);
            } else if (event.type === diffusion.clients.SessionEventStreamEventType.STATE) {
                console.log(`Session state changed: id=${event.sessionId}, state=${event.state}`);
            } else {
                console.log(
                    `Session properties changed: id=${event.sessionId}, properties=${JSON.stringify(event.changedProperties)}`
                );
            }
        },
        onClose: () => {
            console.log('Stream closed');
        },
        onError: (err) => {
            console.error(`An error occured: ${err}`);
        }
    };
    await session1.clients.addSessionEventListener(sessionEventStream, {
        properties: new Set(diffusion.clients.PropertyKeys.ALL_FIXED_PROPERTIES),
        filter: `$Principal NE 'admin'`
    });

    await session2.closeSession();
    await session1.closeSession();
}
