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
/// tag::log
const { PartiallyOrderedCheckpointTester, promiseWithResolvers } = require('../../../../test/util');
/// end::log

export async function sessionTreesUseCase() {
    /// tag::log
    const check = new PartiallyOrderedCheckpointTester([[
        'Session1 subscribed to my/personal/path',
        'Session2 subscribed to my/personal/path',
        'Session1 value changed to Good morning Administrator',
        'Session2 value changed to Good night Anonymous',
    ]]);
    const promiseValue1 = promiseWithResolvers();
    const promiseValue2 = promiseWithResolvers();
    /// end::log
    /// tag::session_trees_use_case[]
    let session;
    try {
        // Connect to the server.
        session = await diffusion.connect({
            host: 'localhost',
            port: 8080,
            principal: 'admin',
            credentials: 'password'
        });
    } catch (err) {
        console.error('Connection could not be established.', err);
        throw err;
    }

    const specification = new diffusion.topics.TopicSpecification(diffusion.topics.TopicType.STRING);
    try {
        await session.topicUpdate.set(
            'my/topic/path/for/admin',
            diffusion.datatypes.string(),
            'Good morning Administrator',
            { specification: specification }
        );

        await session.topicUpdate.set(
            'my/topic/path/for/control',
            diffusion.datatypes.string(),
            'Good afternoon Control Client',
            { specification: specification }
        );

        await session.topicUpdate.set(
            'my/topic/path/for/anonymous',
            diffusion.datatypes.string(),
            'Good night Anonymous',
            { specification: specification }
        );
    } catch (err) {
        console.error('Topic values could not be set.', err);
        throw err;
    }

    try {
        const branchMappingTable = diffusion.newBranchMappingTableBuilder()
            .addBranchMapping('$Principal is "admin"', 'my/topic/path/for/admin')
            .addBranchMapping('$Principal is "control"', 'my/topic/path/for/control')
            .addBranchMapping('$Principal is ""', 'my/topic/path/for/anonymous')
            .create('my/personal/path');
        await session.sessionTrees.putBranchMappingTable(branchMappingTable);
    } catch (err) {
        console.error('Could not put branch mapping table', err);
    }

    const valueStream = session.addStream('my/personal/path', diffusion.datatypes.string());
    valueStream.on({
        subscribe : (topic, specification) => {
            console.log(`Subscribed to ${topic}`);
            /// tag::log
            check.log(`Session1 subscribed to ${topic}`);
            /// end::log
        },
        unsubscribe : (topic, specification, reason) => {
            console.log(`Unsubscribed from ${topic}: ${reason}`);
        },
        value : (topic, spec, newValue, oldValue) => {
            console.log(`${topic} changed from ${oldValue} to ${newValue}`);
            /// tag::log
            check.log(`Session1 value changed to ${newValue}`);
            promiseValue1.resolve();
            /// end::log
        }
    });

    try {
        await session.select('my/personal/path');
    } catch (err) {
        console.error('Could not select topic.', err);
        throw err;
    }

    let session2;
    try {
        // Connect to the server.
        session2 = await diffusion.connect({
            host: 'localhost',
            port: 8080
        });
    } catch (err) {
        console.error('Connection could not be established.', err);
        throw err;
    }

    const valueStream2 = session2.addStream('my/personal/path', diffusion.datatypes.string());
    valueStream2.on({
        subscribe : (topic, specification) => {
            console.log(`Subscribed to ${topic}`);
            /// tag::log
            check.log(`Session2 subscribed to ${topic}`);
            /// end::log
        },
        unsubscribe : (topic, specification, reason) => {
            console.log(`Unsubscribed from ${topic}: ${reason}`);
        },
        value : (topic, spec, newValue, oldValue) => {
            console.log(`${topic} changed from ${oldValue} to ${newValue}`);
            /// tag::log
            check.log(`Session2 value changed to ${newValue}`);
            promiseValue2.resolve();
            /// end::log
        }
    });

    try {
        await session2.select('my/personal/path');
    } catch (err) {
        console.error('Could not select topic.', err);
        throw err;
    }

    /// tag::log
    await Promise.all([promiseValue1.promise, promiseValue2.promise]);
    /// end::log
    try {
        await session.closeSession();
        await session2.closeSession();
    } catch (err) {
        console.error('An error occurred when closing session.', err);
    }
    /// end::session_trees_use_case[]
    /// tag::log
    await check.done();
    /// end::log
}
