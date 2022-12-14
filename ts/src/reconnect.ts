/*******************************************************************************
 * Copyright (C) 2019 - 2022 Push Technology Ltd.
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

import { connect, Session } from 'diffusion';

// example showcasing how to create a custom reconnection strategy
export async function reconnectExample(): Promise<void> {

    // When establishing a session, it is possible to specify whether reconnection
    // should be attempted in the event of an unexpected disconnection. This allows
    // the session to recover its previous state.

    // Set the maximum amount of time we'll try and reconnect for to 10 minutes
    const maximumTimeoutDuration = 1000 * 60 * 10;

    // Set the maximum interval between reconnect attempts to 60 seconds
    const maximumAttemptInterval = 1000 * 60;

    // Set an upper limit to the number of times we'll try to reconnect for
    const maximumAttempts = 25;

    // Count the number of reconnection attempts we've made
    let attempts = 0;

    // Create a reconnection strategy that applies an exponential back-off
    // The strategy will be called with two arguments, start & abort. Both
    // of these are functions, which allow the strategy to either start a
    // reconnection attempt, or to abort reconnection (which will close the session)
    const reconnectionStrategy = (start: () => void, abort: () => void) => {
        if (attempts > maximumAttempts) {
            abort();
        } else {
            const wait = Math.min(Math.pow(2, attempts++) * 100, maximumAttemptInterval);

            // Wait the specified time period, and then start the reconnection attempt
            setTimeout(start, wait);
        }
    };

    // Connect to the server.
    const session: Session = await connect({
        host: 'diffusion.example.com',
        port: 443,
        secure: true,
        principal: 'control',
        credentials: 'password',
        reconnect: {
            timeout: maximumTimeoutDuration,
            strategy: reconnectionStrategy
        }
    });

    session.on('disconnect', function(reason) {
        // This will be called when we lose connection. Because we've specified the
        // reconnection strategy, it will be called automatically when this event
        // is dispatched
    });

    session.on('reconnect', function() {
        // If the session is able to reconnect within the reconnect timeout, this
        // event will be dispatched to notify that normal operations may resume
        attempts = 0;
    });

    session.on('close', function() {
        // If the session is closed normally, or the session is unable to reconnect,
        // this event will be dispatched to notify that the session is no longer
        // operational.
    });
}
