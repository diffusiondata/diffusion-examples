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

import { connect, datatypes, MissingTopicHandler, topics } from 'diffusion';
/// tag::log
import { PartiallyOrderedCheckpointTester, promiseWithResolvers } from '../../../test/util';
/// end::log

export async function monitoringMissingTopicNotifications(): Promise<void> {
    /// tag::log
    const check = new PartiallyOrderedCheckpointTester([
        [ 'missing topic notification my/topic/path/does/not/exist/yet' ],
        [ 'Subscribed to my/topic/path/does/not/exist/yet' ],
    ]);
    const promise = promiseWithResolvers<void>();
    /// end::log
    /// tag::monitoring_missing_topic_notifications[]
    // Connect to the server.
    const session1 = await connect({
        host: 'localhost',
        port: 8080,
        principal: 'admin',
        credentials: 'password'
    });

    const missingTopicHandler: MissingTopicHandler = {
        onMissingTopic: async (notification) => {
            /// tag::log
            check.log(`missing topic notification ${notification.path}`);
            /// end::log
            const specification = new topics.TopicSpecification(topics.TopicType.STRING);
            await session1.topics.add(notification.path, specification);
        },
        onRegister: (path, deregister) => {},
        onClose: (path) => {},
        onError: (path, error) => {}
    };
    await session1.topics.addMissingTopicHandler('my/topic/path', missingTopicHandler);

    // Connect to the server.
    const session2 = await connect({
        host: 'localhost',
        port: 8080,
        principal: 'client',
        credentials: 'password'
    });

    const valueStream = session2.addStream('?my/topic/path//', datatypes.string());
    valueStream.on({
        subscribe : (topic, specification) => {
            console.log(`Subscribed to ${topic}`);
            /// tag::log
            check.log(`Subscribed to ${topic}`);
            promise.resolve();
            /// end::log
        },
        unsubscribe : (topic, specification, reason) => {
            console.log(`Unsubscribed from ${topic}: ${reason}`);
        },
        value : (topic, spec, newValue, oldValue) => {
            console.log(`${topic} changed from ${oldValue.get()} to ${newValue.get()}`);
        }
    });

    await session2.select('my/topic/path/does/not/exist/yet');

    // wait until topic has been created
    /// tag::log
    await promise.promise;
    /// end::log

    await session1.closeSession();
    await session2.closeSession();
    /// end::monitoring_missing_topic_notifications[]
    /// tag::log
    await check.done();
    /// end::log
}
