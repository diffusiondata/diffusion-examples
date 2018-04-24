//  Diffusion Client Library for iOS, tvOS and OS X / macOS - Examples
//
//  Copyright (C) 2016, 2017 Push Technology Ltd.
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

#import "RemoveTopicsWithSessionExample.h"

@import Diffusion;

@interface RemoveTopicsWithSessionExample () <PTDiffusionTopicTreeRegistrationDelegate>
@end

@implementation RemoveTopicsWithSessionExample {
    PTDiffusionSession* _session;
}

-(void)startWithURL:(NSURL*)url {
    PTDiffusionCredentials *const credentials =
        [[PTDiffusionCredentials alloc] initWithPassword:@"password"];

    PTDiffusionSessionConfiguration *const sessionConfiguration =
        [[PTDiffusionSessionConfiguration alloc] initWithPrincipal:@"control"
                                                       credentials:credentials];

    NSLog(@"Connecting...");

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
        _session = session;

        PTDiffusionTopicControlFeature *const tc = session.topicControl;

        // Add a topic so we can register it for removal on session close.
        [tc addWithTopicPath:@"Example/Auto Removed"
                        type:PTDiffusionTopicType_JSON
           completionHandler:^(NSError *const error)
        {
            if (error) {
                NSLog(@"Failed to add topic. Error: %@", error);
            } else {
                NSLog(@"Topic added.");
            }
        }];

        // Register to remove the Example topic tree when the session closes.
        [tc removeTopicsWithSessionForTopicPath:@"Example"
                                       delegate:self
                              completionHandler:^(PTDiffusionTopicTreeRegistration *const registration, NSError *const error)
        {
            if (registration) {
                NSLog(@"Registered.");
            } else {
                NSLog(@"Registration failed. Error: %@", error);
            }
        }];
    }];
}

-(void)diffusionTopicTreeRegistrationDidClose:(PTDiffusionTopicTreeRegistration *const)registration {
    NSLog(@"Closed");
}

-(void)diffusionTopicTreeRegistration:(PTDiffusionTopicTreeRegistration *const)registration
                     didFailWithError:(NSError *const)error {
    NSLog(@"Failed: %@", error);
}

@end
