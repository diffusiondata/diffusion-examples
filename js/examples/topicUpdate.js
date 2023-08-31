/*******************************************************************************
 * Copyright (C) 2018 - 2023 DiffusionData Ltd.
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
const session;

const stringDataType = diffusion.datatypes.string();
const jsonDataType = diffusion.datatypes.json();
const intDataType = diffusion.datatypes.int64();
const TopicType = diffusion.topics.TopicType;
const UpdateConstraintOperator = diffusion.topicUpdate.UpdateConstraintOperator;


function basicSetOperation() {
    return Promise.all([
        session.topicUpdate.set('foo_topic', stringDataType, 'hello'),
        session.topicUpdate.set('bar_topic', jsonDataType, {foo: 'foo', qux: 'qux'})
    ]);
}

function setOperationWithValueConstraint() {
    const constraint = diffusion.updateConstraints().value('hello', stringDataType);
    return session.topicUpdate.set('foo_topic', stringDataType, 'world', {constraint});
}

function setOperationWithSessionLockConstraint() {
    return session.lock("lock")
        .then(function(sessionLock){
            const constraint = diffusion.updateConstraints().locked(sessionLock);
            return session.topicUpdate.set('foo_topic', stringDataType, 'lorem ipsum', {constraint});
        });
}

function setOperationWithPartialJSONConstraint() {
    const constraint = diffusion.updateConstraints().jsonValue()
        .with('/foo', 'foo', UpdateConstraintOperator.IS, stringDataType)
        .without('/bar');
    return session.topicUpdate.set('bar_topic', jsonDataType, {foo:'baz', bar:'bar'}, {constraint});
}

function setOperationWithPartialJSONConstraintOnInt() {
    const constraint = updateConstraints().jsonValue()
        .with('/sequence', 42, topicUpdate.UpdateConstraintOperator.LT, intDataType)
        .without('/bar');
    return session.topicUpdate.set('bar_topic', jsonDataType, {sequence: 42, bar:'bar'}, {constraint});
}

function basicAddAndSetOperation() {
    const topicSpec = new diffusion.topics.TopicSpecification(TopicType.STRING);
    return session.topicUpdate.set('baz_topic', stringDataType, 'hello', {specification: topicSpec});
}

function addAndSetOperationWithNoTopicConstraint() {
    const topicSpec = new diffusion.topics.TopicSpecification(TopicType.STRING);
    const constraint = diffusion.updateConstraints().noTopic();
    return session.topicUpdate.set('qux_topic', stringDataType, 'hello', {specification: topicSpec, constraint});
}

function createUpdateStream() {
    const stream = session.topicUpdate.newUpdateStreamBuilder()
        .build('foo_topic', stringDataType);
    stream.validate();
    stream.set('hello');
    const cachedValue = stream.get();
    return stream.set('world');
}

function createUpdateStreamWithValueConstraint() {
    const constraint = diffusion.updateConstraints().value('world', stringDataType);
    const stream = session.topicUpdate.newUpdateStreamBuilder()
        .constraint(constraint)
        .build('foo_topic', stringDataType);
    stream.validate();
    stream.set('hello');
    const cachedValue = stream.get();
    return stream.set('world');
}


function createUpdateStreamThatAddsTopic() {
    const topicSpec = new diffusion.topics.TopicSpecification(TopicType.STRING);
    const stream = session.topicUpdate.newUpdateStreamBuilder()
        .specification(topicSpec)
        .build('quux_topic', stringDataType);
    // the first call to validate() or set() resolves in a TopicCreationResult
    stream.validate().then((result) => {
        if (result === diffusion.topicUpdate.TopicCreationResult.CREATED) {
            console.log('A new topic has been created!');
        } else {
            console.log('The topic already existed.');
        }
    });
    stream.set('hello');
    const cachedValue = stream.get();
    return stream.set('world');
}

function createUpdateStreamThatAddsTopicWithNoTopicConstraint() {
    const topicSpec = new diffusion.topics.TopicSpecification(TopicType.STRING);
    const constraint = diffusion.updateConstraints().noTopic();
    const stream = session.topicUpdate.newUpdateStreamBuilder()
        .specification(topicSpec)
        .constraint(constraint)
        .build('quuz_topic', stringDataType);
    // the first call to validate() or set() resolves in a TopicCreationResult
    stream.validate().then((result) => {
        if (result === diffusion.topicUpdate.TopicCreationResult.CREATED) {
            console.log('A new topic has been created!');
        } else {
            console.log('The topic already existed.');
        }
    });
    stream.set('hello');
    const cachedValue = stream.get();
    return stream.set('world');
}

// Connect to the server. Change these options to suit your own environment.
// Node.js does not accept self-signed certificates by default. If you have
// one of these, set the environment variable NODE_TLS_REJECT_UNAUTHORIZED=0
// before running this example.
diffusion.connect({
    host   : 'diffusion.example.com',
    port   : 443,
    secure : true,
    principal : 'control',
    credentials : 'password'
})
.then(function(sess) {
    session = sess;
    return Promise.all([
        session.topics.add('foo_topic', TopicType.STRING),
        session.topics.add('bar_topic', TopicType.JSON)
    ]);
})
.then(basicSetOperation)
.then(setOperationWithValueConstraint)
.then(setOperationWithSessionLockConstraint)
.then(setOperationWithPartialJSONConstraint)
.then(setOperationWithPartialJSONConstraintOnInt)
.then(basicAddAndSetOperation)
.then(addAndSetOperationWithNoTopicConstraint)
.then(createUpdateStream)
.then(createUpdateStreamWithValueConstraint)
.then(createUpdateStreamThatAddsTopic)
.then(createUpdateStreamThatAddsTopicWithNoTopicConstraint);
