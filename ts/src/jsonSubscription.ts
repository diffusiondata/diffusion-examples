/*******************************************************************************
 * Copyright (C) 2019 Push Technology Ltd.
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

import { connect, datatypes, topics, Session } from 'diffusion';

// example showcasing how to receive updates for JSON topics
export async function jsonSubscriptionExample() {

    // Connect to the server. Change these options to suit your own environment.
    // Node.js will not accept self-signed certificates by default. If you have
    // one of these, set the environment variable NODE_TLS_REJECT_UNAUTHORIZED=0
    // before running this example.
    const session: Session = await connect({
        host: 'diffusion.example.com',
        port: 443,
        secure: true
    });

    // 1. ValueStreams are the best way to receive values from JSON and Binary topics.

    // Streams for notifications can be registered separately from subscribing to a topic. Registering a stream is a
    // local operation and does not change the data the client receives.

    // Like subscribing, streams are registered using a topic selection. Each stream will only receive notifications
    // from topics that match the topic selector it is registered with.

    // A ValueStream will only receive notifications from topics that match the topic selector it is registered
    // with and that match its DataType.

    // A ValueStream emits a 'value' notification, listeners for 'value' events will be passed the topic path,
    // topic specification, the new value of the topic and the previous value.

    // A ValueStream also emits 'open', 'close', 'subscribe' and 'unsubscribe' events.

    session
        .addStream('foo', datatypes.json())
        .on('value', function(path, specification, newValue, oldValue) {
            console.log('Got JSON update for topic: ' + path, newValue.get());
        });

    // 2. Subscribe to the "foo" topic. The value stream registered earlier will now start to receive notifications.
    session.select('foo');
}
