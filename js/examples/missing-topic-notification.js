/*******************************************************************************
 * Copyright (C) 2018, 2021 Push Technology Ltd.
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

// Connect to the server. Change these options to suit your own environment.
// Node.js will not accept self-signed certificates by default. If you have
// one of these, set the environment variable NODE_TLS_REJECT_UNAUTHORIZED=0
// before running this example.
diffusion.connect({
    host   : 'diffusion.example.com',
    port   : 443,
    secure : true
}).then(function(session) {
	var stringDataType = diffusion.datatypes.string();
	var TopicType = diffusion.topics.TopicType;
	var TopicSpecification = diffusion.topics.TopicSpecification;

	// Register a missing topic handler on the 'example' root topic
	// Any subscriptions to missing topics along this path will invoke this handler
	session.topics.addMissingTopicHandler('example', {
		// Called when a handler is successfully registered
		onRegister : function(path, close) {
            console.log(`Registered missing topic handler on path: ${path}`);

			// Once we've registered the handler, we subscribe with the selector '?example/topic/.*'
			session.select('?example/topic/.*');

			// Register a stream to listen for a subscription event
			session.addStream('?example/topic/.*', stringDataType).on('subscribe', function(topic, specification) {
                console.log(`Subscribed to topic: ${topic}`);
			});
		},
		// Called when the handler is closed
		onClose : function(path) {
            console.log(`Missing topic handler on path '${path}' has been closed`);
		},
		// Called if there is an error on the handler
		onError : function(path, error) {
			console.log('Error on missing topic handler');
		},
		// Called when we've received a missing topic notification on our registered handler path
		onMissingTopic : function(notification) {
            console.log(`Received missing topic notification with selector: ${notification.selector}`);

			// Once we've received the missing topic notification initiated from subscribing to '?example/topic/.*',
			// we add a topic that will match the selector
			var topic = 'example/topic/foo';

			session.topics.add(topic, new TopicSpecification(TopicType.STRING)).then(function(result) {
                console.log(`Topic add success: ${path}`);
			}, function(reason) {
                console.log(`Topic add failed: ${reason}`);
			});
		}
	});

});
