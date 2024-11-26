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

import {
    connect,
    datatypes,
    SessionEvent,
    SessionEventStream,
    topics,
    clients
} from 'diffusion';

export async function sessionManagementSubscriptionControl(): Promise<void> {
    // Connect to the server.
    const session1 = await connect({
        host: 'localhost',
        port: 8080,
        principal: 'admin',
        credentials: 'password'
    });

    const specification = new topics.TopicSpecification(topics.TopicType.STRING);
    await session1.topicUpdate.set(
        'my/topic/path/hello',
        datatypes.string(),
        'Hello World!',
        { specification: specification }
    );
    console.log('Topic value has been set.');

    let deferredUnsubscribeResolve: () => void;
    const unsubscribePromise = new Promise<void>((resolve) => {
        deferredUnsubscribeResolve = resolve;
    });

    const sessionEventStream: SessionEventStream = {
        onSessionEvent: (event: SessionEvent) => {
            console.log(`Session ${event.sessionId.toString()} has been updated:`);
            console.log(`  Type: ${clients.SessionEventStreamEventType[event.type]}`);
            console.log(`  State: ${clients.SessionState[event.state]}`);

            if (
                event.type === clients.SessionEventStreamEventType.STATE
                && event.state === clients.SessionState.ACTIVE
                && event.sessionId.toString() !== session1.sessionId.toString()
            ) {
                session1.clients.subscribe(event.sessionId, '?my/topic/path//');
                setTimeout(async () => {
                    await session1.clients.unsubscribe(event.sessionId, '?my/topic/path//');
                    deferredUnsubscribeResolve();
                }, 5000);
            }
        },
        onClose: () => {},
        onError: () => {}
    };
    await session1.clients.addSessionEventListener(sessionEventStream, {});

    // Connect to the server.
    const session2 = await connect({
        host: 'localhost',
        port: 8080,
        principal: 'client',
        credentials: 'password'
    });

    const valueStream = session2.addFallbackStream(datatypes.string());
    valueStream.on({
        subscribe : (topic, specification) => {
            console.log(`Subscribed to ${topic}`);
        },
        unsubscribe : (topic, specification, reason) => {
            console.log(`Unsubscribed from ${topic}: ${reason}`);
        },
        value : (topic, spec, newValue, oldValue) => {
            console.log(`${topic} changed from ${oldValue.get()} to ${newValue.get()}`);
        }
    });

    await unsubscribePromise;

    await session1.closeSession();
    await session2.closeSession();
}
