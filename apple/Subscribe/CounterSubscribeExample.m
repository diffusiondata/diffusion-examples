//  Diffusion Client Library for iOS, tvOS and OS X / macOS - Examples
//
//  Copyright (C) 2015 - 2023 DiffusionData Ltd.
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

#import "CounterSubscribeExample.h"

@import Diffusion;

@interface CounterSubscribeExample (PTDiffusionNumberValueStreamDelegate) <PTDiffusionNumberValueStreamDelegate>
@end

@implementation CounterSubscribeExample {
    PTDiffusionSession* _session;
}

-(void)startWithURL:(NSURL*)url {

    NSLog(@"Connecting...");

    [PTDiffusionSession openWithURL:url
                  completionHandler:^(PTDiffusionSession * const session, NSError * const error)
    {
        if (!session) {
            NSLog(@"Failed to open session: %@", error);
            return;
        }

        // At this point we now have a connected session.
        NSLog(@"Connected.");

        // Set ivar to maintain a strong reference to the session.
        self->_session = session;

        // Register self as the fallback handler for topic updates.
        PTDiffusionValueStream *const stream =
            [PTDiffusionPrimitive int64NumberValueStreamWithDelegate:self];
        NSError *fallbackError;
        if (![session.topics addFallbackStream:stream error:&fallbackError]) {
            NSLog(@"Error while adding fallback stream: %@", fallbackError.description);
        }

        NSLog(@"Subscribing...");
        [session.topics subscribeWithTopicSelectorExpression:@"foo/counter"
                                           completionHandler:^(NSError * const error)
        {
            if (error) {
                NSLog(@"Subscribe request failed. Error: %@", error);
            } else {
                NSLog(@"Subscribe request succeeded.");
            }
        }];
    }];
}

@end

@implementation CounterSubscribeExample (PTDiffusionNumberValueStreamDelegate)

-(void)     diffusionStream:(PTDiffusionStream *const)stream
    didSubscribeToTopicPath:(NSString *const)topicPath
              specification:(PTDiffusionTopicSpecification *const)specification {
    NSLog(@"Subscribed: %@", topicPath);
}

-(void)diffusionStream:(PTDiffusionValueStream *const)stream
    didUpdateTopicPath:(NSString *const)topicPath
         specification:(PTDiffusionTopicSpecification *const)specification
             oldNumber:(NSNumber *const)oldNumber
             newNumber:(NSNumber *const)newNumber {
    NSLog(@"The value of %@ is: %@", topicPath, newNumber);
}

-(void)         diffusionStream:(PTDiffusionStream *)stream
    didUnsubscribeFromTopicPath:(NSString *)topicPath
                  specification:(PTDiffusionTopicSpecification *)specification
                         reason:(PTDiffusionTopicUnsubscriptionReason)reason {
    NSLog(@"Unsubscribed: %@", topicPath);
}

-(void)diffusionDidCloseStream:(PTDiffusionStream *const)stream {
    NSLog(@"Closed");
}

-(void)diffusionStream:(PTDiffusionStream *const)stream
      didFailWithError:(NSError *const)error {
    NSLog(@"Failed: %@", error);
}

@end
