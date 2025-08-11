//  Diffusion Client Library for iOS, tvOS and OS X / macOS - Examples
//
//  Copyright (C) 2016 - 2023 DiffusionData Ltd.
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//  http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

#import "CustomReconnectionStrategyExample.h"

@import Diffusion;

@interface ExponentialBackoffReconnectionStrategy : NSObject <PTDiffusionSessionReconnectionStrategy>
@end

@implementation CustomReconnectionStrategyExample {
    PTDiffusionSession* _session;
}

-(void)startWithURL:(NSURL*)url {
    NSLog(@"Connecting...");

    PTDiffusionMutableSessionConfiguration *const sessionConfiguration =
        [PTDiffusionMutableSessionConfiguration new];

    // Set the maximum amount of time we'll try and reconnect for to 10 minutes.
    sessionConfiguration.reconnectionTimeout = @(10.0 * 60.0); // seconds

    // Set the reconnection strategy to be used.
    sessionConfiguration.reconnectionStrategy = [ExponentialBackoffReconnectionStrategy new];

    // Start connecting asynchronously.
    [PTDiffusionSession openWithURL:url
                      configuration:sessionConfiguration
                  completionHandler:^(PTDiffusionSession *session, NSError *error)
    {
        if (!session) {
            NSLog(@"Failed to open session: %@", error);
            return;
        }

        // At this point we now have a connected session.
        NSLog(@"Connected.");

        // Set ivar to maintain a strong reference to the session.
        self->_session = session;
    }];
}

@end

@implementation ExponentialBackoffReconnectionStrategy {
    NSUInteger _attemptCount;
}

-(void)         diffusionSession:(PTDiffusionSession *const)session
    wishesToReconnectWithAttempt:(PTDiffusionSessionReconnectionAttempt *const)attempt {
    // Limit the maximum time to delay between reconnection attempts to 60 seconds.
    const NSTimeInterval maximumAttemptInterval = 60.0;

    // Compute delay for exponential backoff based on the number of attempts so far.
    const NSTimeInterval delay = MIN(pow(2.0, _attemptCount++) * 0.1, maximumAttemptInterval);

    // Schedule asynchronous execution.
    NSLog(@"Reconnection attempt scheduled for %.2fs", delay);
    dispatch_after(dispatch_time(DISPATCH_TIME_NOW, (int64_t)(delay * NSEC_PER_SEC)),
        dispatch_get_main_queue(), ^
    {
        NSLog(@"Attempting reconnection.");
        [attempt start];
    });
}

@end
