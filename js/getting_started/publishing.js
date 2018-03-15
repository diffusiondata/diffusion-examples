/*******************************************************************************
 * Copyright (C) 2014, 2017 Push Technology Ltd.
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

diffusion.connect({
    host : 'localhost',
    port : '8080',
    principal : 'control',
    credentials : 'password'
}).then(function(session) {
    console.log('Connected!');

    var i = 0;
        
    // Create a JSON topic
    session.topics.add("foo/counter", diffusion.topics.TopicType.JSON);
  
    // Start updating the topic every second
   setInterval(function() { 
    session.topics.update("foo/counter", { count : i++ }); 
	}, 1000);
   
});