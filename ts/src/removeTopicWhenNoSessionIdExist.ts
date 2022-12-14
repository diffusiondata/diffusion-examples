/*******************************************************************************
 * Copyright (C) 2019 - 2022 Push Technology Ltd.
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

import { connect, datatypes, topics, Session, TopicSpecification } from 'diffusion';

// example showcasing how to automatically remove a topic using a topic removal specification
export async function removeTopicExample(): Promise<void> {

    // Connect to the server. Change these options to suit your own environment.
    // Node.js does not accept self-signed certificates by default. If you have
    // one of these, set the environment variable NODE_TLS_REJECT_UNAUTHORIZED=0
    // before running this example.
    const session: Session = await connect({
        host: 'diffusion.example.com',
        port: 443,
        secure: true,
        principal: 'control',
        credentials: 'password'
    });

    // Subscribe to topic 'foo'
    await session.select("foo");

    // Register a JSON value stream and listen for subscription/unsubscription events
    const stream = session.addStream("foo", datatypes.json());
    stream.on({
        'subscribe': function (topic, spec) {
            console.log("Subscribed to topic: ", topic);
        },
        'unsubscribe': function (topic, spec, reason) {
            console.log("Unsubscribed from topic: ", topic);
            console.log("Reason: ", reason);

            // Finally close the session
            session.close();
        }
    });

    // Remove a topic 2 seconds when no clients with principal 'unknown'
    const expression = `when no session has '$Principal is "unknown"' for 2s`;

    // Add a JSON topic type with the REMOVAL topic specification property
    const jsonSpec = new topics.TopicSpecification(topics.TopicType.JSON)
        .withProperty(topics.TopicSpecification.REMOVAL, expression);

    try {
        await session.topics.add("foo", jsonSpec);
        console.log("Topic added");
    } catch (error) {
        console.log("Failed ", error);
    }
}
