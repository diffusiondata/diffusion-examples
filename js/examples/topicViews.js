/*******************************************************************************
 * Copyright (C) 2020 - 2023 DiffusionData Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the 'License');
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an 'AS IS' BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/

const diffusion = require('diffusion');
let session;

async function createTopicView() {
    // First create a topic called 'some/topic'.
    await session.topics.add('some/topic', diffusion.topics.TopicType.STRING);

    // Now add a topic view.
    // This will create a topic 'other/topic' that mirrors the value of 'some/topic'
    await session.topicViews.createTopicView('example-view', 'map ?some/ to other/<path(1)>');
}

async function listTopicViews() {
    // list all topic views
    const views = await session.topicViews.listTopicViews();

    // show the name and specification of each topic view
    console.log('All Topic Views:');
    views.forEach((topicView) => {
        console.log(`${topicView.name}: ${topicView.specification}`);
    });
}

async function getTopicView() {
    // get a topic view
    const topicView = await session.topicViews.getTopicView('example-view');

    // show the name and specification of the topic view
    console.log('Topic View:');
    console.log(`${topicView.name}: ${topicView.specification}`);
}

async function removeTopicViews() {
    // remove a named topic view
    await session.topicViews.removeTopicView('example-view');
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
})
.then(createTopicView)
.then(listTopicViews)
.then(getTopicView)
.then(removeTopicViews);
