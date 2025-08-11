/*******************************************************************************
 * Copyright (C) 2025 Diffusion Data Ltd.
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

import { connect, datatypes, topics } from 'diffusion';
/// tag::log
import { PartiallyOrderedCheckpointTester } from '../../../../test/util'
/// end::log

export async function subscribeTopicSelectionScopesExample(): Promise<void> {
    /// tag::log
    const check = new PartiallyOrderedCheckpointTester([
        [
            'componentA: Subscribed to my/topic/path',
            'componentB: Subscribed to my/topic/path'
        ],
        ['componentB: Subscribed to my/other/path'],
        [
            'componentA: Unsubscribed from my/topic/path',
            'componentB: Unsubscribed from my/topic/path',
            'componentB: Unsubscribed from my/other/path'
        ]
    ]);
    /// end::log
    /// tag::pub_sub_subscribe_selection_scopes[]
    // Connect to the server.
    const session = await connect({
        host: 'localhost',
        port: 8080,
        principal: 'admin',
        credentials: 'password'
    });

    // Create topics 'my/topic/path' and 'my/other/path'
    const specification = new topics.TopicSpecification(topics.TopicType.STRING);
    await session.topics.add('my/topic/path', specification);
    await session.topics.add('my/other/path', specification);

    // each component registers a stream for the topics they are interested in
    // and subscribe to the topics with their own scope

    const componentAStream = session.addStream('my/topic/path', datatypes.string());
    const componentBStream = session.addStream('?my//', datatypes.string());

    componentAStream.on({
        subscribe : (topic, specification) => {
            console.log(`componentA: Subscribed to ${topic}`);
            /// tag::log
            check.log(`componentA: Subscribed to ${topic}`);
            /// end::log
        },
        unsubscribe : (topic, specification, reason) => {
            console.log(`componentA: Unsubscribed from ${topic}`);
            /// tag::log
            check.log(`componentA: Unsubscribed from ${topic}`);
            /// end::log
        },
        close : () => {
            console.log(`componentA: stream closed`);
        },
        value : (topic, spec, newValue, oldValue) => {
            console.log(`componentA: ${topic} changed from ${oldValue?.get()} to ${newValue?.get()}`);
        }
    });

    componentBStream.on({
        subscribe : (topic, specification) => {
            console.log(`componentB: Subscribed to ${topic}`);
            /// tag::log
            check.log(`componentB: Subscribed to ${topic}`);
            /// end::log
        },
        unsubscribe : (topic, specification, reason) => {
            console.log(`componentB: Unsubscribed from ${topic}`);
            /// tag::log
            check.log(`componentB: Unsubscribed from ${topic}`);
            /// end::log
        },
        close : () => {
            console.log(`componentB: stream closed`);
        },
        value : (topic, spec, newValue, oldValue) => {
            console.log(`componentB: ${topic} changed from ${oldValue?.get()} to ${newValue?.get()}`);
        }
    });

    await session.selectWithScope('my/topic/path', 'scopeA');
    await session.selectWithScope('my/topic/path', 'scopeB');
    await session.selectWithScope('my/other/path', 'scopeB');

    const selectionsA = await session.clients.getTopicSelections(session.sessionId);
    for (const selection of Object.keys(selectionsA)) {
        console.log(`Scope ${selection}: ${selectionsA[selection].map(topic => topic.selector).join(', ')}`);
    }

    // componentB unsubscribes from 'my/topic/path', scopeA is unaffected
    await session.unsubscribeWithScope('my/topic/path', 'scopeB');

    const selectionsB = await session.clients.getTopicSelections(session.sessionId);
    for (const selection of Object.keys(selectionsB)) {
        console.log(`Scope ${selection}: ${selectionsB[selection].map(topic => topic.selector).join(', ')}`);
    }

    // componentA unsubscribes from all topics, scopeB is unaffected
    // 'my/topic/path' is no longer in any scopes and is unsubscribed for the session
    await session.unsubscribeWithScope("?.*//", "scopeA");

    // a control client unsubscribes all remaining scopes for 'my/other/path' which
    // is now unsubscribed for the session
    await session.clients.unsubscribeAllScopes(session.sessionId, 'my/other/path');

    await session.closeSession();
    /// end::pub_sub_subscribe_selection_scopes[]
    /// tag::log
    await check.done();
    /// end::log
}
