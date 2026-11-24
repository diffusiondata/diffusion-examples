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

import { connect, datatypes, newBranchMappingTableBuilder, Session, topics } from 'diffusion';

export async function sessionTreesUseCase(): Promise<void> {
    let session: Session;
    try {
        // Connect to the server.
        session = await connect({
            host: 'localhost',
            port: 8080,
            principal: 'admin',
            credentials: 'password'
        });
    } catch (err) {
        console.error('Connection could not be established.', err);
        throw err;
    }

    const specification = new topics.TopicSpecification(topics.TopicType.STRING);
    try {
        await session.topicUpdate.set(
            'my/topic/path/for/admin',
            datatypes.string(),
            'Good morning Administrator',
            { specification: specification }
        );

        await session.topicUpdate.set(
            'my/topic/path/for/control',
            datatypes.string(),
            'Good afternoon Control Client',
            { specification: specification }
        );

        await session.topicUpdate.set(
            'my/topic/path/for/anonymous',
            datatypes.string(),
            'Good night Anonymous',
            { specification: specification }
        );
    } catch (err) {
        console.error('Topic values could not be set.', err);
        throw err;
    }

    try {
        const branchMappingTable = newBranchMappingTableBuilder()
            .addBranchMapping('$Principal is "admin"', 'my/topic/path/for/admin')
            .addBranchMapping('$Principal is "control"', 'my/topic/path/for/control')
            .addBranchMapping('$Principal is ""', 'my/topic/path/for/anonymous')
            .create('my/personal/path');
        await session.sessionTrees.putBranchMappingTable(branchMappingTable);
    } catch (err) {
        console.error('Could not put branch mapping table', err);
    }

    const valueStream = session.addStream('my/personal/path', datatypes.string());
    valueStream.on({
        subscribe : (topic, specification) => {
            console.log(`Subscribed to ${topic}`);
        },
        unsubscribe : (topic, specification, reason) => {
            console.log(`Unsubscribed from ${topic}: ${reason}`);
        },
        value : (topic, spec, newValue, oldValue) => {
            console.log(`${topic} changed from ${oldValue} to ${newValue}`);
        }
    });

    try {
        await session.select('my/personal/path');
    } catch (err) {
        console.error('Could not select topic.', err);
        throw err;
    }

    let session2: Session;
    try {
        // Connect to the server.
        session2 = await connect({
            host: 'localhost',
            port: 8080
        });
    } catch (err) {
        console.error('Connection could not be established.', err);
        throw err;
    }

    const valueStream2 = session2.addStream('my/personal/path', datatypes.string());
    valueStream2.on({
        subscribe : (topic, specification) => {
            console.log(`Subscribed to ${topic}`);
        },
        unsubscribe : (topic, specification, reason) => {
            console.log(`Unsubscribed from ${topic}: ${reason}`);
        },
        value : (topic, spec, newValue, oldValue) => {
            console.log(`${topic} changed from ${oldValue} to ${newValue}`);
        }
    });

    try {
        await session2.select('my/personal/path');
    } catch (err) {
        console.error('Could not select topic.', err);
        throw err;
    }

    try {
        await session.closeSession();
        await session2.closeSession();
    } catch (err) {
        console.error('An error occurred when closing session.', err);
    }
}
