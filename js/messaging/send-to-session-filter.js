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

export async function messagingSendToSessionFilter() {
    // Connect to the server.
    const session1 = await diffusion.connect({
        host: 'localhost',
        port: 8080,
        principal: 'admin',
        credentials: 'password'
    });

    const stream1 = {
        onRequest: (path, request, responder) => {
            console.log(`Received message: ${request}`);
            responder.respond('Goodbye');
        },
        onError: (error) => {
            console.error('An error occurred.', error);
        },
        onClose: () => {
            console.log('Message handler was closed.');
        }
    };

    session1.messages.setRequestStream('my/message/path', stream1);

    // Connect to the server.
    const session2 = await diffusion.connect({
        host: 'localhost',
        port: 8080,
        principal: 'control',
        credentials: 'password'
    });

    const stream2 = {
        onRequest: (path, request, responder) => {
            console.log(`Received message: ${request}`);
            responder.respond('I\'m not supposed to receive a message.');
        },
        onError: (error) => {
            console.error('An error occurred.', error);
        },
        onClose: () => {
            console.log('Message handler was closed.');
        }
    };

    session2.messages.setRequestStream('my/message/path',stream2);

    // Connect to the server.
    const session3 = await diffusion.connect({
        host: 'localhost',
        port: 8080,
        principal: 'control',
        credentials: 'password'
    });

    await session3.messages.sendRequestToFilter(
        '$Principal EQ "admin"',
        'my/message/path',
        'Hello',
        {
            onResponse: (sessionId, response)=> {
                console.log(`Received response: ${response}`);
            },
            onResponseError: (sessionId, error) => {
                console.error('Received an error response', error);
            }
        }
    );

    await session1.closeSession();
    await session2.closeSession();
    await session3.closeSession();
}
