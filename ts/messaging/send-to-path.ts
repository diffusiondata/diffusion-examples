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

import { connect, RequestHandler } from 'diffusion';
/// tag::log
import { PartiallyOrderedCheckpointTester, promiseWithResolvers } from '../../../test/util';
/// end::log

export async function messagingSendToPath(): Promise<void> {
    /// tag::log
    const check = new PartiallyOrderedCheckpointTester([
        ['Received message: Hello'],
        ['Received response: Goodbye'],
        ['Message handler was closed.'],
    ]);

    const closedPromise = promiseWithResolvers<void>();
    /// end::log
    /// tag::messaging_send_to_path[]
    // Connect to the server.
    const session1 = await connect({
        host: 'localhost',
        port: 8080,
        principal: 'admin',
        credentials: 'password'
    });

    const handler: RequestHandler = {
        onRequest: (request, context, responder) => {
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
            /// tag::log
            check.log('Message handler was closed.');
            closedPromise.resolve();
            /// end::log
        }
    };

    await session1.messages.addRequestHandler('my/message/path', handler);

    // Connect to the server.
    const session2 = await connect({
        host: 'localhost',
        port: 8080,
        principal: 'admin',
        credentials: 'password'
    });

    const response = await session2.messages.sendRequest('my/message/path', 'Hello');
    console.log(`Received response: ${response}`);
    /// tag::log
    check.log(`Received response: ${response}`);
    /// end::log

    await session1.closeSession();
    await session2.closeSession();
    /// end::messaging_send_to_path[]
    /// tag::log
    await closedPromise.promise;
    await check.done();
    /// end::log
}
