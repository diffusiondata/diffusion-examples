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
const { expectTopicCounts } = require('../../../../test/util');
/// end::log

export async function pubSubAutomaticTopicRemoval() {
    /// tag::pub_sub_remove_automatic_topic_removal[]
    // Connect to the server.
    const session = await diffusion.connect({
        host: 'localhost',
        port: 8080,
        principal: 'admin',
        credentials: 'password'
    });

    const specification1 = new diffusion.topics.TopicSpecification(diffusion.topics.TopicType.JSON, {
        REMOVAL: 'when time after "Tue, 4 May 2077 11:05:30 GMT"'
    });
    const topicCreationResult1 = await session.topics.add('my/topic/path/to/be/removed/time/after', specification1);
    if (topicCreationResult1.added) {
        console.log(`Topic "${topicCreationResult1.topic}" has been created.`);
    } else {
        console.log('Topic already exists.');
    }
    /// tag::log
    expect(topicCreationResult1.added).toBeTrue();
    /// end::log

    const specification2 = new diffusion.topics.TopicSpecification(diffusion.topics.TopicType.JSON, {
        REMOVAL: 'when subscriptions < 1 for 10m'
    });
    const topicCreationResult2 = await session.topics.add('my/topic/path/to/be/removed/subscriptions', specification2);
    if (topicCreationResult2.added) {
        console.log(`Topic "${topicCreationResult2.topic}" has been created.`);
    } else {
        console.log('Topic already exists.');
    }
    /// tag::log
    expect(topicCreationResult2.added).toBeTrue();
    /// end::log

    const specification3 = new diffusion.topics.TopicSpecification(diffusion.topics.TopicType.JSON, {
        REMOVAL: 'when local subscriptions < 1 for 10m'
    });
    const topicCreationResult3 = await session.topics.add('my/topic/path/to/be/removed/local/subscriptions', specification3);
    if (topicCreationResult3.added) {
        console.log(`Topic "${topicCreationResult3.topic}" has been created.`);
    } else {
        console.log('Topic already exists.');
    }
    /// tag::log
    expect(topicCreationResult3.added).toBeTrue();
    /// end::log

    const specification4 = new diffusion.topics.TopicSpecification(diffusion.topics.TopicType.JSON, {
        REMOVAL: 'when no updates for 10m'
    });
    const topicCreationResult4 = await session.topics.add('my/topic/path/to/be/removed/no/updates', specification4);
    if (topicCreationResult4.added) {
        console.log(`Topic "${topicCreationResult4.topic}" has been created.`);
    } else {
        console.log('Topic already exists.');
    }
    /// tag::log
    expect(topicCreationResult4.added).toBeTrue();
    /// end::log

    const specification5 = new diffusion.topics.TopicSpecification(diffusion.topics.TopicType.JSON, {
        REMOVAL: 'when no session has \'$Principal is "client"\' for 1h'
    });
    const topicCreationResult5 = await session.topics.add('my/topic/path/to/be/removed/no/session', specification5);
    if (topicCreationResult5.added) {
        console.log(`Topic "${topicCreationResult5.topic}" has been created.`);
    } else {
        console.log('Topic already exists.');
    }
    /// tag::log
    expect(topicCreationResult5.added).toBeTrue();
    /// end::log

    const specification6 = new diffusion.topics.TopicSpecification(diffusion.topics.TopicType.JSON, {
        REMOVAL: 'when no local session has \'Department is "Accounts"\' for 1h after 1d'
    });
    const topicCreationResult6 = await session.topics.add('my/topic/path/to/be/removed/no/local/session', specification6);
    if (topicCreationResult6.added) {
        console.log(`Topic "${topicCreationResult6.topic}" has been created.`);
    } else {
        console.log('Topic already exists.');
    }
    /// tag::log
    expect(topicCreationResult6.added).toBeTrue();
    /// end::log

    const specification7 = new diffusion.topics.TopicSpecification(diffusion.topics.TopicType.JSON, {
        REMOVAL: 'when subscriptions < 1 for 10m or no updates for 20m'
    });
    const topicCreationResult7 = await session.topics.add('my/topic/path/to/be/removed/subcriptions/or/updates', specification7);
    if (topicCreationResult7.added) {
        console.log(`Topic "${topicCreationResult7.topic}" has been created.`);
    } else {
        console.log('Topic already exists.');
    }
    /// tag::log
    expect(topicCreationResult7.added).toBeTrue();
    /// end::log

    const specification8 = new diffusion.topics.TopicSpecification(diffusion.topics.TopicType.JSON, {
        REMOVAL: 'when subscriptions < 1 for 10m and no updates for 20m'
    });
    const topicCreationResult8 = await session.topics.add('my/topic/path/to/be/removed/subcriptions/and/updates', specification8);
    if (topicCreationResult8.added) {
        console.log(`Topic "${topicCreationResult8.topic}" has been created.`);
    } else {
        console.log('Topic already exists.');
    }
    /// tag::log
    expect(topicCreationResult8.added).toBeTrue();
    /// end::log

    await session.closeSession();
    /// end::pub_sub_remove_automatic_topic_removal[]
    /// tag::log
    await expectTopicCounts({
        'my/topic/path/to/be/removed/time/after': 1,
        'my/topic/path/to/be/removed/subscriptions': 1,
        'my/topic/path/to/be/removed/local/subscriptions': 1,
        'my/topic/path/to/be/removed/no/updates': 1,
        'my/topic/path/to/be/removed/no/session': 1,
        'my/topic/path/to/be/removed/no/local/session': 1,
        'my/topic/path/to/be/removed/subcriptions/or/updates': 1,
        'my/topic/path/to/be/removed/subcriptions/and/updates': 1
    });
    /// end::log
}
