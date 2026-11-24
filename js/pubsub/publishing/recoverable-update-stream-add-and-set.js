/*******************************************************************************
 * Copyright (C) 2026 Diffusion Data Ltd.
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

export async function recoverableUpdateSreamAddAndSetExample() {
    // Connect to the server.
    const session = await diffusion.connect({
        host: 'localhost',
        port: 8080,
        principal: 'admin',
        credentials: 'password'
    });

    const specification = new diffusion.topics.TopicSpecification(diffusion.topics.TopicType.JSON);

    // A recoverable update stream is defined by a RetryStrategy. This example uses a RetryController,
    // providing access to the retryAttempts counter (zero based) for dynamic control.
    // Cater for up to 10 errors during streamed updates, with each successive retry waiting a second longer.
    const retryStrategy = (retryAttempts) => {
        if (retryAttempts > 9) {
            return null;
        }
        return (retryAttempts + 1) * 1000;
    };

    const updateStream = session.topicUpdate.newUpdateStreamBuilder()
        .specification(specification)
        .build('my/topic/path/with/recoverable/update/stream', diffusion.datatypes.json(), retryStrategy);

    const promises = [ ...Array(10).keys() ]
        .map((i) => {
            const jsonData = diffusion.datatypes.json().from({ counter: i });
            return updateStream.set(jsonData);
        });

    try {
        await Promise.all(promises);
    } catch (err) {
        if (updateStream.isRecoverable()) {
            await updateStream.recover(); // Initiate retry-limited recovery, to deliver pending topic updates.
        } else {
            console.error('Cannot recover', err);
        }
    }

    await session.closeSession();
}
