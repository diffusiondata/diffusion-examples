/*******************************************************************************
 * Copyright (C) 2019, 2021 Push Technology Ltd.
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

 import { connect, datatypes, topics, topicUpdate, updateConstraints, Session, SessionLock } from 'diffusion';

 // example showcasing how to update topics using session.topicUpdate.set or topic update streams
 export async function topicUpdateExample() {

    const stringDataType = datatypes.string();
    const TopicType = topics.TopicType;


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


    async function basicSetOperation() {
        await session.topicUpdate.set('foo_topic', stringDataType, 'hello');
    }

    async function setOperationWithValueConstraint() {
        const constraint = updateConstraints().value('hello', stringDataType);
        await session.topicUpdate.set('foo_topic', stringDataType, 'world', {constraint});
    }

    async function setOperationWithSessionLockConstraint() {
        const sessionLock: SessionLock = await session.lock("lock");
        const constraint = updateConstraints().locked(sessionLock);

        await session.topicUpdate.set('foo_topic', stringDataType, 'lorem ipsum', {constraint});
    }

    async function setOperationWithPartialJSONConstraint() {
        const sessionLock: SessionLock = await session.lock("lock");
        const constraint = updateConstraints().jsonValue()
            .with('/foo', stringDataType, 'foo')
            .without('/bar');
        await session.topicUpdate.set('foo_topic', datatypes.json(), {foo:'baz', bar:'bar'}, {constraint});
    }

    async function basicAddAndSetOperation() {
        const topicSpec = new topics.TopicSpecification(TopicType.STRING);
        await session.topicUpdate.set('bar_topic', stringDataType, 'hello', {specification: topicSpec});
    }

    async function addAndSetOperationWithNoTopicConstraint() {
        const topicSpec = new topics.TopicSpecification(TopicType.STRING);
        const constraint = updateConstraints().noTopic();
        await session.topicUpdate.set('bar_topic', stringDataType, 'hello', {specification: topicSpec, constraint});
    }

    async function createUpdateStream() {
        const stream = session.topicUpdate.createUpdateStream('foo_topic', stringDataType);
        await stream.validate();
        await stream.set('hello');
        const cachedValue = stream.get();
        await stream.set('world');
    }

    async function createUpdateStreamWithValueConstraint() {
        const constraint = updateConstraints().value('hello', stringDataType);
        const stream = session.topicUpdate.createUpdateStream('foo_topic', stringDataType, {constraint});
        await stream.validate();
        await stream.set('hello');
        const cachedValue = stream.get();
        return stream.set('world');
    }


    async function createUpdateStreamThatAddsTopic() {
        const topicSpec = new topics.TopicSpecification(TopicType.STRING);
        const stream = session.topicUpdate.createUpdateStream('quux_topic', stringDataType, {specification: topicSpec});
        // the first call to validate() or set() resolves in a TopicCreationResult
        const result = await stream.validate();
        if (result === topicUpdate.TopicCreationResult.CREATED) {
            console.log('A new topic has been created!');
        } else {
            console.log('The topic already existed.');
        }
        await stream.set('hello');
        const cachedValue = stream.get();
        await stream.set('world');
    }

    async function createUpdateStreamThatAddsTopicWithValueConstraint() {
        const topicSpec = new topics.TopicSpecification(TopicType.STRING);
        const constraint = updateConstraints().noTopic();
        const stream = session.topicUpdate.createUpdateStream('quuz_topic', stringDataType, {specification: topicSpec, constraint});
        // the first call to validate() or set() resolves in a TopicCreationResult
        const result = await stream.validate();
        if (result === topicUpdate.TopicCreationResult.CREATED) {
            console.log('A new topic has been created!');
        } else {
            console.log('The topic already existed.');
        }
        await stream.set('hello');
        const cachedValue = stream.get();
        await stream.set('world');
    }

    await session.topics.add('foo_topic', TopicType.STRING);

    await basicSetOperation;
    await setOperationWithValueConstraint;
    await setOperationWithSessionLockConstraint;
    await setOperationWithPartialJSONConstraint;
    await basicAddAndSetOperation;
    await addAndSetOperationWithNoTopicConstraint;
    await createUpdateStream;
    await createUpdateStreamWithValueConstraint;
    await createUpdateStreamThatAddsTopic;
    await createUpdateStreamThatAddsTopicWithValueConstraint;
}
