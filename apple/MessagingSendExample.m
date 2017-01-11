//  Diffusion Client Library for iOS and OS X - Examples
//
//  Copyright (C) 2015, 2016 Push Technology Ltd.
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

#import "MessagingSendExample.h"

@import Diffusion;

@implementation MessagingSendExample {
    PTDiffusionSession* _session;
    NSUInteger _nextValue;
}

-(void)startWithURL:(NSURL*)url {
    NSLog(@"Connecting...");

    [PTDiffusionSession openWithURL:url
                  completionHandler:^(PTDiffusionSession *session, NSError *error)
     {
         if (!session) {
             NSLog(@"Failed to open session: %@", error);
             return;
         }

         // At this point we now have a connected session.
         NSLog(@"Connected.");

         // Set ivar to maintain a strong reference to the session.
         _session = session;
         
         // Create a timer to send a message once a second.
         NSTimer *const timer = [NSTimer timerWithTimeInterval:1.0
                                                        target:self
                                                      selector:@selector(sendMessage:)
                                                      userInfo:session
                                                       repeats:YES];
         [[NSRunLoop currentRunLoop] addTimer:timer forMode:NSDefaultRunLoopMode];
     }];
}

-(void)sendMessage:(NSTimer *const)timer {
    PTDiffusionSession *const session = timer.userInfo;

    const NSUInteger value = _nextValue++;
    NSData *const data = [[NSString stringWithFormat:@"%lu", (long)value] dataUsingEncoding:NSUTF8StringEncoding];
    PTDiffusionContent *const content = [[PTDiffusionContent alloc] initWithData:data];

    NSLog(@"Sending %lu...", (long)value);
    [session.messaging sendWithTopicPath:@"foo/bar"
                                   value:content
                                 options:[PTDiffusionSendOptions new]
                       completionHandler:^(NSError *const error)
    {
        if (error) {
            NSLog(@"Failed to send. Error: %@", error);
        } else {
            NSLog(@"Sent");
        }
    }];
}

@end
