/*******************************************************************************
 * Copyright (C) 2016 Push Technology Ltd.
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

// A JavaScript Subscription will provide you with a Buffer of unparsed data
// unless you transform the Subscription using metadata, functions or
// datatypes. A transformed Subscription will give you a stream of parsed
// values.

// Here we create a RecordContent metadata without any structure. It specifies
// any number of records and fields and can be used to parse a Buffer
// containing any RecordContent. Records and fields both need names to help
// look them up later.
var unstructured = new diffusion.metadata.RecordContent();
unstructured
    // This adds any number of records with the name 'r'.
    .addRecord('r', {min: 1, max: -1})
    // This adds any number of fields with the name 'f' to the record 'r'.
    .addField('f', new diffusion.metadata.String(), {min: 1, max: -1});

// Here we define a function that traverses a RecordContent object and returns
// an array of arrays. Each record is represented as an array of fields. The
// update is represented as an array of record arrays. We will use this
// function to transform a RecordContent stream into an Array stream.
function recordContentToArray(recordContentUpdate) {
    // You can all access records and fields in different ways. Records are
    // accessed through the RecordContent and fields are accessed through a
    // Record. You can call forEach to invoke a function for every record or
    // field. You can call either records or fields with the name to get an
    // array. You can call get with the name and index to access the entry
    // directly.

    var updateArray = [];

    // Iterate over the records of the update using a function.
    recordContentUpdate.forEach(function (record) {
        // Look up and store the array of fields named 'f'.
        updateArray.push(record.fields('f'));
    });

    return updateArray;
}

// Connect to the server. Change these options to suit your own environment.
// Node.js will not accept self-signed certificates by default. If you have
// one of these, set the environment variable NODE_TLS_REJECT_UNAUTHORIZED=0
// before running this example.
diffusion.connect({
    host   : 'diffusion.example.com',
    port   : 443,
    secure : true
}).then(function(session) {

    // Subscribe to a topic.
    var bufferStream = session.subscribe('>test');

    // Transform the Buffer stream to a RecordContent stream using metadata.
    var recordContentStream = bufferStream.transform(unstructured);

    // Transform the RecordContent stream to an Array stream using a function.
    var arrayStream = recordContentStream.transform(recordContentToArray);

    // Add the update event listener
    arrayStream.on({
        update : function (arrayUpdate) {
            console.log('Message: ', arrayUpdate);
        }
    });
});
