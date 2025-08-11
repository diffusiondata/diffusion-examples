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

#import "MissingTopicHandlerExample.h"

@import Diffusion;

@interface MissingTopicHandlerExample (PTDiffusionMissingTopicHandler) <PTDiffusionMissingTopicHandler>
@end

@implementation MissingTopicHandlerExample {
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
        self->_session = session;

        // Register as missing topic handler for a branch of the topic tree.
        [self registerAsMissingTopicHandlerForSession:session];
    }];
}

-(void)registerAsMissingTopicHandlerForSession:(PTDiffusionSession *const)session {
    [session.topicControl addMissingTopicHandler:self
                                    forTopicPath:@"Example/Control Client Handler"
                               completionHandler:^(PTDiffusionTopicTreeRegistration *const registration, NSError *const error)
    {
        if (registration) {
            NSLog(@"Registered as missing topic handler.");
        } else {
            NSLog(@"Failed to register as missing topic handler. Error: %@", error);
        }
    }];
}

@end

@implementation MissingTopicHandlerExample (PTDiffusionMissingTopicHandler)

-(void)diffusionTopicTreeRegistration:(PTDiffusionTopicTreeRegistration *const)registration
          hadMissingTopicNotification:(PTDiffusionMissingTopicNotification *const)notification {
    NSString *const expression = notification.topicSelectorExpression;
    NSLog(@"Received Missing Topic Notification: %@", expression);

    // Expect a path pattern expression.
    if (![expression hasPrefix:@">"]) {
        NSLog(@"Topic selector expression is not a path pattern.");
        return;
    }

    // Extract topic path from path pattern expression.
    NSString *const topicPath = [expression substringFromIndex:1];

    // Add a topic at this path.
    [_session.topicControl addTopicWithPath:topicPath
                                       type:PTDiffusionTopicType_JSON
                          completionHandler:^(PTDiffusionAddTopicResult *const result, NSError *const error)
    {
        if (error) {
            NSLog(@"Failed to add topic.");
            return;
        }
    }];
}

-(void)diffusionTopicTreeRegistrationDidClose:(PTDiffusionTopicTreeRegistration *)registration {
    NSLog(@"Closed");
}

-(void)diffusionTopicTreeRegistration:(PTDiffusionTopicTreeRegistration *)registration
                     didFailWithError:(NSError *)error {
    NSLog(@"Failed: %@", error);
}

@end
