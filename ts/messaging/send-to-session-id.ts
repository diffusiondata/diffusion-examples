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

import { connect, RequestStream } from 'diffusion';
/// tag::log
const { PartiallyOrderedCheckpointTester } = require('../../../test/util');
/// end::log

export async function messagingSendToSessionId(): Promise<void> {
    /// tag::log
    const check = new PartiallyOrderedCheckpointTester([
        ['Received message: Hello'],
        ['Received response: Goodbye'],
    ]);
    /// end::log
    /// tag::messaging_send_to_session_id[]
    // Connect to the server.
    const session1 = await connect({
        host: 'localhost',
        port: 8080,
        principal: 'admin',
        credentials: 'password'
    });

    const stream1: RequestStream = {
        onRequest: (path, request, responder) => {
            console.log(`Received message: ${request}`);
            /// tag::log
            check.log(`Received message: ${request}`);
            /// end::log
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
    const session2 = await connect({
        host: 'localhost',
        port: 8080,
        principal: 'admin',
        credentials: 'password'
    });

    const stream2: RequestStream = {
        onRequest: (path, request, responder) => {
            console.log(`Received message: ${request}`);
            responder.respond('I\'m not supposed to receive a message.');
            /// tag::log
            check.log('I\'m not supposed to receive a message.');
            /// end::log
        },
        onError: (error) => {
            console.error('An error occurred.', error);
        },
        onClose: () => {
            console.log('Message handler was closed.');
        }
    };

    session2.messages.setRequestStream('my/message/path', stream2);

    // Connect to the server.
    const session3 = await connect({
        host: 'localhost',
        port: 8080,
        principal: 'admin',
        credentials: 'password'
    });
    console.log(`Session 3 connected ${session3.sessionId}`);

    const response = await session3.messages.sendRequest(
        'my/message/path',
        'Hello',
        session1.sessionId);
    console.log(`Received response: ${response}`);
    /// tag::log
    check.log(`Received response: ${response}`);
    /// end::log

    await session1.closeSession();
    await session2.closeSession();
    await session3.closeSession();
    /// end::messaging_send_to_session_id[]
    /// tag::log
    await check.done();
    /// end::log
}
