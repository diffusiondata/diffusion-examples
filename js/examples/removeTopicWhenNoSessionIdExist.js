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

var diffusion = require('diffusion');

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
}).then(function(session) {
    // Subscribe to topic 'foo'
    session.select("foo");

    // Register a JSON value stream and listen for subscription/unsubscription events
    var stream = session.addStream("foo", diffusion.datatypes.json());
    stream.on({
        'subscribe': function (topic, spec) {
            console.log("Subscribed to topic: ", topic);
        },
        'unsubscribe': function (topic, spec, reason) {
            console.log("Unsubscribed from topic: ", topic);
            console.log("Reason: ", reason);

            // Finally close the session
            session.close();
        }
    });

    // Remove a topic 2 seconds when no clients with principal 'unknown'
    var expression = "when no session has '$Principal is \"unknown\"' for 2s";

    // Add a JSON topic type with the REMOVAL topic specification property
    var jsonSpec = new diffusion.topics.TopicSpecification(diffusion.topics.TopicType.JSON)
        .withProperty(diffusion.topics.TopicSpecification.REMOVAL, expression);

    session.topics.add("foo", jsonSpec).then(function() {
        console.log("Topic added");
    }, function(err) {
        console.log("Failed ", err);
    });
});
