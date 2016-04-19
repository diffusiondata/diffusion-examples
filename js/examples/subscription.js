/*******************************************************************************
 * Copyright (C) 2014, 2015 Push Technology Ltd.
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

    // 1. Subscriptions are how sessions receive streams of data from the server. 
    
    // When subscribing, a topic selector is used to select which topics to subscribe to. Topics do not need to exist 
    // at the time of subscription - the server dynamically resolves subscriptions as topics are added or removed.

    // Subscribe to the "foo" topic with an inline callback function
    var subscription = session.subscribe('foo', function(update) {
        // Log the new value whenever the 'foo' topic is updated
        // By default, we get a Buffer object which preserves binary
        // data.
        console.log(update);
    });

    // Callbacks can also be registered after the subscription has occurred
    subscription.on({
        update : function(value, topic, sub) {
            console.log('Update for topic: ' + topic, value);
        },
        subscribe : function(details, topic, sub) {
            console.log('Subscribed to topic: ' + topic);
        },
        unsubscribe : function(reason, topic, sub) {
            console.log('Unsubscribed from topic:' + topic);
            sub.close();
        }
    });

    // 2. Sessions may unsubscribe from any topic to stop receiving data
    
    // Unsubscribe from the "foo" topic. Sessions do not need to have previously been subscribed to the topics they are
    // unsubscribing from. Unsubscribing from a topic will result in the 'unsubscribe' callback registered above being
    // called.
    session.unsubscribe('foo');

    // 3. Subscriptions / Unsubscriptions can select multiple topics using Topic Selectors
    
    // Topic Selectors provide regex-like capabilities for subscribing to topics. These are resolved dynamically, much
    // like subscribing to a single topic.
    var subscription2 = session.subscribe('?foo/.*/[a-z]');
    
    // 4. Subscriptions can use transformers to convert update values
    
    // Subscribe to a topic and then convert all received values to JSON. Transforming a subscription creates a new
    // subscription stream, rather than modifying the original.
    session.subscribe('bar').transform(JSON.parse).on('update', function(value, topic) {
        console.log('Got JSON update for topic: ' + topic, value);
    });

    // 5. Metadata can be used within transformers to parse data
   
    // Create a simple metadata instance
    var meta = new diffusion.metadata.RecordContent();

    // Add a single record/field
    meta.addRecord('record', {
        'field' : meta.string('some-value')
    });

    // Subscribe to a topic and transform with the metadata
    session.subscribe('baz').transform(meta).on('update', function(value) {
        console.log('Field value: ', value.get('record').get('field'));
    });
});
