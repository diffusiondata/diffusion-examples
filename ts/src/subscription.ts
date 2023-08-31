/*******************************************************************************
 * Copyright (C) 2019 - 2023 DiffusionData Ltd.
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

import { connect, datatypes, Session, ValueStream } from 'diffusion';

// example showcasing how to subscribe to a topic with a value stream
export async function subscriptionExample(): Promise<void> {

    // Connect to the server. Change these options to suit your own environment.
    // Node.js will not accept self-signed certificates by default. If you have
    // one of these, set the environment variable NODE_TLS_REJECT_UNAUTHORIZED=0
    // before running this example.
    const session: Session = await connect({
        host: 'diffusion.example.com',
        port: 443,
        secure: true
    });

    // 1. Subscriptions are how sessions receive streams of data from the server.

    // When subscribing, a topic selector is used to select which topics to subscribe to. Topics do not need to exist
    // at the time of subscription - the server dynamically resolves subscriptions as topics are added or removed.

    // Subscribe to the "foo" topic
    await session.select('foo');

    // 2. Sessions may unsubscribe from any topic to stop receiving data

    // Unsubscribe from the "foo" topic. Sessions do not need to have previously been subscribed to the topics they are
    // unsubscribing from.
    await session.unsubscribe('foo');

    // 3. Subscriptions / Unsubscriptions can select multiple topics using Topic Selectors

    // Topic Selectors provide regex-like capabilities for subscribing to topics. These are resolved dynamically, much
    // like subscribing to a single topic.
    session.select('?foo/.*/[a-z]');

    // 4. Register a value stream
    const valueStream: ValueStream = session.addStream('baz', datatypes.json());

    // Receive update values
    valueStream.on('value', function(topic, spec, newValue, oldValue) {
        console.log('JSON update for topic: ' + topic, newValue.get());
    });

    // Subscribe to a JSON topic
    session.select('baz');
}
