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
    host        : 'localhost',
    port        : 8080,
    secure      : false,
    principal   : 'control',
    credentials : 'password'
}).then(function(session) {

    // Assuming a topic tree:
    //
    // scores
    //   |-- football
    //   |     |-- semi1
    //   |     |-- semi2
    //   |     |-- final
    //   |
    //   |-- tennis
    //         |-- semi1
    //         |-- semi2
    //         |-- final

    // Use a regular expression to create a view of the topics tracking the
    // scores during the finals for each sport.
    var view = session.view('?scores/.*/final');

    // Alternatively, we can use a topic set. Note that the topics do not need
    // to be under a common root, they may be anywhere within the topic tree.
    var view2 = session.view('#>scores/football/final////>scores/tennis/final');

    // If any of the topics in the view change, display which topic changed
    // and its new value.
    view.on({
        update : function(value) {
            // Get and print the entire view structure.
            console.log('Update: ', JSON.stringify(value, undefined, 4));

            // Get individual topics. Returns a Buffer, which is automatically
            // converted to a String during concatenation, below.
            //
            // Note that the structure may not exist if the value has not been
            // updated.
            console.log('Football score: ' + value.scores.football.final);
            console.log('Tennis score  : ' + value.scores.tennis.final);

            // or ...
            // console.log('Football score: ' + value['scores']['football']['final']);
        }
    });

    // The structure can also be accessed outside the update event.
    console.log('Football score: ' + view.get().scores.football.final);
});
