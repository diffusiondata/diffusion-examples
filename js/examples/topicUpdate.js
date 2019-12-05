/*******************************************************************************
 * Copyright (C) 2018 Push Technology Ltd.
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

var diffusion = require('diffusion');
var session;

var stringDataType = diffusion.datatypes.string();
var jsonDataType = diffusion.datatypes.json();
var TopicType = diffusion.topics.TopicType;


function basicSetOperation() {
    return Promise.all([
        session.topicUpdate.set('foo_topic', stringDataType, 'hello'),
        session.topicUpdate.set('bar_topic', jsonDataType, {foo: 'foo', qux: 'qux'})
    ]);
}

function setOperationWithValueConstraint() {
    var constraint = diffusion.updateConstraints().value('hello', stringDataType);
    return session.topicUpdate.set('foo_topic', stringDataType, 'world', {constraint});
}

function setOperationWithSessionLockConstraint() {
    return session.lock("lock")
        .then(function(sessionLock){
            var constraint = diffusion.updateConstraints().locked(sessionLock);
            return session.topicUpdate.set('foo_topic', stringDataType, 'lorem ipsum', {constraint});
        });
}

function setOperationWithPartialJSONConstraint() {
    return session.lock("lock")
        .then(function(sessionLock){
            var constraint = diffusion.updateConstraints().jsonValue()
                .with('/foo', 'foo', stringDataType)
                .without('/bar');
            return session.topicUpdate.set('bar_topic', jsonDataType, {foo:'baz', bar:'bar'}, {constraint});
        });
}

function basicAddAndSetOperation() {
    var topicSpec = new diffusion.topics.TopicSpecification(TopicType.STRING);
    return session.topicUpdate.set('baz_topic', stringDataType, 'hello', {specification: topicSpec});
}

function addAndSetOperationWithNoTopicConstraint() {
    var topicSpec = new diffusion.topics.TopicSpecification(TopicType.STRING);
    var constraint = diffusion.updateConstraints().noTopic();
    return session.topicUpdate.set('qux_topic', stringDataType, 'hello', {specification: topicSpec, constraint});
}

function createUpdateStream() {
    var stream = session.topicUpdate.createUpdateStream('foo_topic', stringDataType);
    stream.validate();
    stream.set('hello');
    var cachedValue = stream.get();
    return stream.set('world');
}

function createUpdateStreamWithValueConstraint() {
    var constraint = diffusion.updateConstraints().value('world', stringDataType);
    var stream = session.topicUpdate.createUpdateStream('foo_topic', stringDataType, {constraint});
    stream.validate();
    stream.set('hello');
    var cachedValue = stream.get();
    return stream.set('world');
}


function createUpdateStreamThatAddsTopic() {
    var topicSpec = new diffusion.topics.TopicSpecification(TopicType.STRING);
    var stream = session.topicUpdate.createUpdateStream('quux_topic', stringDataType, {specification: topicSpec});
    stream.validate();
    stream.set('hello');
    var cachedValue = stream.get();
    return stream.set('world');
}

function createUpdateStreamThatAddsTopicWithNoTopicConstraint() {
    var topicSpec = new diffusion.topics.TopicSpecification(TopicType.STRING);
    var constraint = diffusion.updateConstraints().noTopic();
    var stream = session.topicUpdate.createUpdateStream('quuz_topic', stringDataType, {specification: topicSpec, constraint});
    stream.validate();
    stream.set('hello');
    var cachedValue = stream.get();
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
.then(basicAddAndSetOperation)
.then(addAndSetOperationWithNoTopicConstraint)
.then(createUpdateStream)
.then(createUpdateStreamWithValueConstraint)
.then(createUpdateStreamThatAddsTopic)
.then(createUpdateStreamThatAddsTopicWithNoTopicConstraint);
