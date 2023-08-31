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

import { connect, datatypes, topics, topicUpdate, updateConstraints, Session, SessionLock } from 'diffusion';

// example showcasing how to update topics using session.topicUpdate.set or topic update streams
export async function topicUpdateExample(): Promise<void> {

    const stringDataType = datatypes.string();
    const jsonDataType = datatypes.json();
    const intDataType = datatypes.int64();
    const TopicType = topics.TopicType;
    const UpdateConstraintOperator = topicUpdate.UpdateConstraintOperator;

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
        await session.topicUpdate.set('bar_topic', jsonDataType, {foo: 'foo', qux: 'qux'})
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
        const constraint = updateConstraints().jsonValue()
            .with('/foo', 'foo', UpdateConstraintOperator.IS, stringDataType)
            .without('/bar');
        await session.topicUpdate.set('bar_topic', jsonDataType, {foo:'baz', bar:'bar'}, {constraint});
    }

    async function setOperationWithPartialJSONConstraintOnInt() {
        const constraint = updateConstraints().jsonValue()
            .with('/sequence', 42, topicUpdate.UpdateConstraintOperator.LT, intDataType)
            .without('/bar');
        await session.topicUpdate.set('bar_topic', jsonDataType, {sequence: 42, bar:'bar'}, {constraint});
    }

    async function basicAddAndSetOperation() {
        const topicSpec = new topics.TopicSpecification(TopicType.STRING);
        await session.topicUpdate.set('baz_topic', stringDataType, 'hello', {specification: topicSpec});
    }

    async function addAndSetOperationWithNoTopicConstraint() {
        const topicSpec = new topics.TopicSpecification(TopicType.STRING);
        const constraint = updateConstraints().noTopic();
        await session.topicUpdate.set('qux_topic', stringDataType, 'hello', {specification: topicSpec, constraint});
    }

    async function createUpdateStream() {
        const stream = session.topicUpdate.newUpdateStreamBuilder()
            .build('foo_topic', stringDataType);
        await stream.validate();
        await stream.set('hello');
        const cachedValue = stream.get();
        await stream.set('world');
    }

    async function createUpdateStreamWithValueConstraint() {
        const constraint = updateConstraints()
            .value(topicUpdate.UpdateConstraintOperator.IS, 'world', stringDataType);
        const stream = session.topicUpdate.newUpdateStreamBuilder()
            .constraint(constraint)
            .build('foo_topic', stringDataType);
        await stream.validate();
        await stream.set('hello');
        const cachedValue = stream.get();
        return stream.set('world');
    }

    async function createUpdateStreamThatAddsTopic() {
        const topicSpec = new topics.TopicSpecification(TopicType.STRING);
        const stream = session.topicUpdate.newUpdateStreamBuilder()
            .specification(topicSpec)
            .build('quux_topic', stringDataType);
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

    async function createUpdateStreamThatAddsTopicWithNoTopicConstraint() {
        const topicSpec = new topics.TopicSpecification(TopicType.STRING);
        const constraint = updateConstraints().noTopic();
        const stream = session.topicUpdate.newUpdateStreamBuilder()
            .specification(topicSpec)
            .constraint(constraint)
            .build('quuz_topic', stringDataType);
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
    await session.topics.add('bar_topic', TopicType.JSON);

    await basicSetOperation();
    await setOperationWithValueConstraint();
    await setOperationWithSessionLockConstraint();
    await setOperationWithPartialJSONConstraint();
    await setOperationWithPartialJSONConstraintOnInt();
    await basicAddAndSetOperation();
    await addAndSetOperationWithNoTopicConstraint();
    await createUpdateStream();
    await createUpdateStreamWithValueConstraint();
    await createUpdateStreamThatAddsTopic();
    await createUpdateStreamThatAddsTopicWithNoTopicConstraint();
}
