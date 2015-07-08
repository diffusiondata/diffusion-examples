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
    // 1. Topics can be created with a specified topic path and value. If the path contains multiple levels, any
    // intermediary topics that do not exist will be created automatically with a stateless topic type.

    // Create a topic with string values, and an initial value of "xyz".
    session.topics.add('topic/string', 'xyz');

    // Create a topic with integer values, and an initial value of 123.
    session.topics.add('topic/integer', 123);

    // Create a topic with decimal values, with an implicit scale of 2 and an initial value of 1.23.
    session.topics.add('topic/decimal', 1.23);

    // 2. Adding a topic returns a result, which allows us to handle when the operation has either 
    // completed successfully or encountered an error.
    session.topics.add('topic/result', 'abc').then(function(result) {
        console.log('Added topic: ' + result.topic);
    }, function(reason) {
        console.log('Failed to add topic: ', reason);
    });

    // Adding a topic that already exists will succeed, so long as it has the same value type
    session.topics.add('topic/result', 'xyz').then(function(result) {
        // result.added will be false, as the topic already existed
        console.log('Added topic: ' + result.topic, result.added);
    });

    // Because the result returned from adding a topic is a promise, we can easily chain
    // multiple topic adds together
    session.topics.add('chain/foo', 1).then(session.topics.add('chain/bar', 2))
                                      .then(session.topics.add('chain/baz', 3))
                                      .then(session.topics.add('chain/bob', 4))
                                      .then(function() {
                                         console.log('Added all topics');
                                      }, function(reason) {
                                         console.log('Failed to add topic', reason);
                                      });

    // 3. Metadata can be used to create topics that will contain values of a specified format.
    
    // RecordContent formats data in a series of records and fields, similar to tabular data.
    // Each record & field is named, allowing direct lookup of values. Each field value has a
    // particular type (string, integer, decimal)
    var metadata = new diffusion.metadata.RecordContent();

    // Records are like rows in a table. They can have fields assigned, with default values. 
    // You can add fields all at once like this, or individually (see below).
    var game = metadata.addRecord('game', 1, {
        'title' : metadata.string(),
        'round' : metadata.integer(0),
        'count' : metadata.integer(0)
    });

    // Records and fields can be set as occurring a certain number of times.
    var player = metadata.addRecord('player', metadata.occurs(0, 8));

    // Add fields to a record individually.
    player.addField('name', 'Anonymous');
    player.addField('score', 0);

    // Adding the topic works just like normal.
    session.topics.add('games/some-game', metadata);

    // And the metadata can be re-used for multiple topics.
    session.topics.add('games/some-other-game', metadata);

    // 4. Using metadata, it is possible to create a topic with both a metadata format, and the initial value
    
    // Topic values can be produced from metadata via the builder interface
    var builder = metadata.builder();

    // Values must be set before a value can be created
    builder.add('game', { title : 'Planet Express!', count : 3 });
    
    builder.add('player', { name : 'Fry', score : 0 });
    builder.add('player', { name : 'Amy', score : 0 });
    builder.add('player', { name : 'Kif', score : 0 });

    // Build a content instance
    var content = builder.build();

    // Now that the content has been built, a topic can be added with the metadata & initial value
    session.topics.add('games/yet-another-game', metadata, content).then(function() {
        console.log('Topic was added with metadata and content');
    });
});
