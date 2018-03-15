//  Diffusion Client Library for iOS and OS X - Examples
//
//  Copyright (C) 2016 Push Technology Ltd.
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

#import "TopicEventListenerExample.h"

@import Diffusion;

@interface TopicEventListenerExample (PTDiffusionTopicEventListener) <PTDiffusionTopicEventListener>
@end

@implementation TopicEventListenerExample {
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

        // Register as topic event listener for a branch of the topic tree.
        [self registerAsTopicEventListenerForSession:session];
    }];
}

-(void)registerAsTopicEventListenerForSession:(PTDiffusionSession *const)session {
    // This example assumes that the topic has already been added.
    // Sessions cannot be subscribed to a topic until it has been created. This,
    // in turn, means that topic events relating to having subscribers cannot
    // be received for a topic that doesn't exist.
    [session.topicControl addTopicEventListener:self
                                   forTopicPath:@"Example/Control Client Listening"
                              completionHandler:^(PTDiffusionTopicTreeRegistration *const registration, NSError *const error)
    {
        if (registration) {
            NSLog(@"Registered as topic event listener.");
        } else {
            NSLog(@"Failed to register as topic event listener. Error: %@", error);
        }
    }];
}

@end

@implementation TopicEventListenerExample (PTDiffusionTopicEventListener)

-(void)diffusionTopicTreeRegistration:(PTDiffusionTopicTreeRegistration *)registration
           hasSubscribersForTopicPath:(NSString *)topicPath {
    NSLog(@"Have subscribers at: %@", topicPath);
}

-(void)diffusionTopicTreeRegistration:(PTDiffusionTopicTreeRegistration *)registration
         hasNoSubscribersForTopicPath:(NSString *)topicPath {
    NSLog(@"Now have no subscribers at: %@", topicPath);
}

@end
