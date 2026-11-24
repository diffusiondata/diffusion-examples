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

export async function monitoringMissingTopicNotifications() {
    // Connect to the server.
    const session1 = await diffusion.connect({
        host: 'localhost',
        port: 8080,
        principal: 'admin',
        credentials: 'password'
    });

    const missingTopicHandler = {
        onMissingTopic: async (notification) => {
            const specification = new diffusion.topics.TopicSpecification(diffusion.topics.TopicType.STRING);
            await session1.topics.add(notification.path, specification);
        },
        onRegister: (path, deregister) => {},
        onClose: (path) => {},
        onError: (path, error) => {}
    };
    await session1.topics.addMissingTopicHandler('my/topic/path', missingTopicHandler);

    // Connect to the server.
    const session2 = await diffusion.connect({
        host: 'localhost',
        port: 8080,
        principal: 'client',
        credentials: 'password'
    });

    const valueStream = session2.addStream('?my/topic/path//', diffusion.datatypes.string());
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

    await session2.select('my/topic/path/does/not/exist/yet');

    // wait until topic has been created

    await session1.closeSession();
    await session2.closeSession();
}
