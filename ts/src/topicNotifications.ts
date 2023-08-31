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

import {
    connect,
    Session,
    TopicNotificationListener,
    TopicSpecification,
    TopicNotificationType,
    TopicNotificationRegistration
} from 'diffusion';

// example showcasing how to fetch topics and their values using session.fetchRequest
export async function fetchRequestExample(): Promise<void> {

    // Connect to the server. Change these options to suit your own environment.
    // Node.js does not accept self-signed certificates by default. If you have
    // one of these, set the environment variable NODE_TLS_REJECT_UNAUTHORIZED=0
    // before running this example.
    const session: Session = await connect({
        host: 'diffusion.example.com',
        port: 443,
        secure: true,
        principal: 'client',
        credentials: 'password'
    });

    const TopicNotificationType = session.notifications.TopicNotificationType;

    // A topic notification listener can be used to listen to topic notifications
    const topicNotificationListener: TopicNotificationListener = {
        // Called when the session receives a notification for a selected topic
        onTopicNotification: (path: string, specification: TopicSpecification, type: TopicNotificationType) => {
            switch (type) {
            case TopicNotificationType.ADDED:
                console.log(`Topic ${path} has been added`);
                break;
            case TopicNotificationType.REMOVED:
                console.log(`Topic ${path} has been removed`);
                break;
            case TopicNotificationType.SELECTED:
                console.log(`Topic ${path} existed at the time of the selector registration.`);
                break;
            case TopicNotificationType.DESELECTED:
                console.log(`Topic ${path} has been deselected`);
                break;
            }
        },
        // Called when the session receives a notification for an immediate
        // descendant of a selected topic
        onDescendantNotification: (path: string, type: TopicNotificationType) => {
            switch (type) {
            case TopicNotificationType.ADDED:
                console.log(`Topic ${path} has been added as a descendant of a selected topic`);
                break;
            case TopicNotificationType.REMOVED:
                console.log(`Topic ${path} has been removed as a descendant of a selected topic`);
                break;
            case TopicNotificationType.SELECTED:
                console.log(`Topic ${path} existed as a descendant of a selected topic at the time of the selector registration.`);
                break;
            case TopicNotificationType.DESELECTED:
                console.log(`Topic ${path} has been deselected as a descendant of a selected topic`);
                break;
            }
        },
        // Called when the listener is closed
        onClose: () => {
            console.log('Topic notification listener has been closed');
        },
        // Called when an error has occurred
        onError(error) {
            console.log('An error has occurred');
        }
    }

    // register the listener
    session.notifications.addListener(topicNotificationListener).then((registration: TopicNotificationRegistration) => {
        // select topics
        // topic notifications will be emitted on all selected topics
        registration.select('?foo/bar//');
    });
}
