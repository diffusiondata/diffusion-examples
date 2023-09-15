/*******************************************************************************
 * Copyright (C) 2022 Push Technology Ltd.
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
package com.pushtechnology.diffusion.examples;

import com.pushtechnology.diffusion.client.Diffusion;
import com.pushtechnology.diffusion.client.session.Session;
import com.pushtechnology.diffusion.client.session.retry.RetryStrategy;

/**
 * This demonstrates a client's use of an initial connection retry strategy.
 *
 * @author DiffusionData Limited
 * @since 6.9
 */
public class ClientUsingInitialConnectionRetryStrategy {

    /**
     * Create a session with an initial retry strategy.
     * <P>
     * Specify the retry interval and number of retry attempts.
     */
    public Session connectWithRetryStrategy(String url, long interval, int attempts) {
        return Diffusion.sessions()
        .initialRetryStrategy(new RetryStrategy(interval, attempts))
        .open(url);
    }

    /**
     *  Specify an initial retry strategy with indefinite retries.
     */
    public Session retryStrategyWithIndefiniteRetries(String url) {
        return Diffusion.sessions()
        .initialRetryStrategy(new RetryStrategy(1))
        .open(url);
    }
}
