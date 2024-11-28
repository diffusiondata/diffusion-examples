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
const { PartiallyOrderedCheckpointTester } = require('../../../test/util');
/// end::log

export async function monitoringTopicNotifications() {
    /// tag::log
    const check = new PartiallyOrderedCheckpointTester([[
        'Topic my/topic/path has been added.',
        'Descendant Topic my/topic/path/descendant has been added.'
    ]]);
    /// end::log
    /// tag::monitoring_topic_notifications[]
    const session = await diffusion.connect({
        host: 'localhost',
        port: 8080,
        principal: 'admin',
        credentials: 'password'
    });

    const topicNotificationListener = {
        onTopicNotification: (path, specification, type) => {
            switch (type) {
                case diffusion.TopicNotificationType.ADDED:
                    console.log(`Topic ${path} has been added.`);
                    /// tag::log
                    check.log(`Topic ${path} has been added.`);
                    /// end::log
                    break;
                case diffusion.TopicNotificationType.SELECTED:
                    console.log(`Topic ${path} has been selected.`);
                    break;
                case diffusion.TopicNotificationType.DESELECTED:
                    console.log(`Topic ${path} has been deselected.`);
                    break;
                case diffusion.TopicNotificationType.REMOVED:
                    console.log(`Topic ${path} has been removed.`);
                    break;
            }
        },
        onDescendantNotification: (path, type) => {
            switch (type) {
                case diffusion.TopicNotificationType.ADDED:
                    console.log(`Descendant Topic ${path} has been added.`);
                    /// tag::log
                    check.log(`Descendant Topic ${path} has been added.`);
                    /// end::log
                    break;
                case diffusion.TopicNotificationType.SELECTED:
                    console.log(`Descendant Topic ${path} has been selected.`);
                    break;
                case diffusion.TopicNotificationType.DESELECTED:
                    console.log(`Descendant Topic ${path} has been deselected.`);
                    break;
                case diffusion.TopicNotificationType.REMOVED:
                    console.log(`Descendant Topic ${path} has been removed.`);
                    break;
            }
        },
        onClose: () => {},
        onError: (error) => {}
    };
    const registration = await session.notifications.addListener(topicNotificationListener);
    await registration.select('>my/topic/path');

    const specification = new diffusion.topics.TopicSpecification(diffusion.topics.TopicType.STRING);
    await session.topicUpdate.set(
        'my/topic/path',
        diffusion.datatypes.string(),
        'Good morning',
        { specification: specification }
    );
    await session.topicUpdate.set(
        'my/topic/path/descendant',
        diffusion.datatypes.string(),
        'Good afternoon',
        { specification: specification }
    );
    await session.topicUpdate.set(
        'other/path/of/the/topic/tree',
        diffusion.datatypes.string(),
        'This will not generate a notification',
        { specification: specification }
    );

    await session.topics.remove('my/topic/path/descendant');

    await session.unsubscribe('?my/topic/path//');

    await session.closeSession();
    /// end::monitoring_topic_notifications[]
    /// tag::log
    await check.done();
    /// end::log
}
