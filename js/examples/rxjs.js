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
var Rx = require('rx');

// Connect to the server. Change these options to suit your own environment.
// Node.js does not accept self-signed certificates by default. If you have
// one of these, set the environment variable NODE_TLS_REJECT_UNAUTHORIZED=0
// before running this example.
diffusion.connect({
   host : 'diffusion.example.com',
   port : 8080,
   principal : 'control',
   credentials : 'password'
}).then(function(session){
    var topic = 'foo';
    var jsonType = diffusion.datatypes.json();

    // Create rxjs observable
    var source = observable(session, topic, jsonType)

    // Subscribe to the rxjs observable
    source.subscribe(
      function (value) {
          console.log('New value: ' + value);
      },
      function (err) {
          console.log('Error: %s', err);
      });

    // Subscribe to the topic
    session.subscribe(topic);
});

// Create and return an observable which creates a stream to receive transformed topic updates on its handler.
// The stream is closed when the subscription to the observable is disposed of.
function observable(session, topic, type) {
  var stream;
  return Rx.Observable.fromEventPattern(
    function(handler) {
      stream = session.stream(topic).asType(type).on({
        value: function(topic, spec, newV, oldV) {
          // Pass the new value to the handler
          handler(newV);
        }
      });
    },
    function() {
      stream.close();
    });
}