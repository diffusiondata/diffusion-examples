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

import { connect, datatypes, Session, topics } from 'diffusion';

export async function pubSubSubscribeCrossCompatible(): Promise<void> {
    // Connect to the server.
    const session = await connect({
        host: 'localhost',
        port: 8080,
        principal: 'admin',
        credentials: 'password'
    });

    const specification = new topics.TopicSpecification(topics.TopicType.INT64);
    await session.topics.add('my/int/topic/path', specification);

    const jsonValueStream = session.addStream('my/int/topic/path', datatypes.json());
    jsonValueStream.on({
        subscribe : (topic, specification) => {
            console.log(`JSON stream subscribed to ${topic}`);
        },
        unsubscribe : (topic, specification, reason) => {
            console.log(`JSON stream unsubscribed from ${topic}: ${reason}`, Date.now(), JSON.stringify(reason));
        },
        value : (topic, spec, newValue, oldValue) => {
            console.log(`JSON stream ${topic} changed from ${oldValue.get()} to ${newValue.get()}`);
        }
    });

    const stringValueStream = session.addStream('my/int/topic/path', datatypes.string());
    stringValueStream.on({
        subscribe : (topic, specification) => {
            console.log(`String stream subscribed to ${topic}`);
        },
        unsubscribe : (topic, specification, reason) => {
            console.log(`String stream unsubscribed from ${topic}: ${reason}`);
        },
        value : (topic, spec, newValue, oldValue) => {
            console.log(`String stream ${topic} changed from ${oldValue} to ${newValue}`);
        }
    });

    console.log('Selecting my/int/topic/path', Date.now());
    await session.select('my/int/topic/path');

    console.log('Closing session', Date.now());
    await session.closeSession();
}
