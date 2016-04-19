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

#import "TopicUpdateExample.h"

@import Diffusion;

@implementation TopicUpdateExample {
    PTDiffusionSession* _session;
}

-(void)start {
    PTDiffusionCredentials *const credentials =
        [[PTDiffusionCredentials alloc] initWithPassword:@"password"];

    PTDiffusionSessionConfiguration *const sessionConfiguration =
        [[PTDiffusionSessionConfiguration alloc] initWithPrincipal:@"control"
                                                       credentials:credentials];

    NSLog(@"Connecting...");

    [PTDiffusionSession openWithURL:_url
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

        // Add topic.
        [self addTopicForSession:session];
    }];
}

static NSString *const _TopicPath = @"Example/Updating";

-(void)addTopicForSession:(PTDiffusionSession *const)session {
    // Add a single value topic without an initial value.
    [session.topicControl addWithTopicPath:_TopicPath
                                      type:PTDiffusionTopicType_SingleValue
                                   content:nil
                         completionHandler:^(NSError * _Nullable error)
    {
        if (error) {
            NSLog(@"Failed to add topic. Error: %@", error);
        } else {
            NSLog(@"Topic created.");

            // Update topic after a short wait.
            [self updateTopicForSession:session withValue:1];
        }
    }];
}

-(void)updateTopicForSession:(PTDiffusionSession *const)session
                   withValue:(const NSUInteger)value {
    dispatch_after(dispatch_time(DISPATCH_TIME_NOW, (int64_t)(1.0 * NSEC_PER_SEC)),
        dispatch_get_main_queue(), ^
    {
        // Get the non-exclusive updater.
        PTDiffusionTopicUpdater *const updater = session.topicUpdateControl.updater;

        // Prepare data to update topic with.
        NSString *const string =
            [NSString stringWithFormat:@"Update #%lu", (unsigned long)value];
        NSData *const data = [string dataUsingEncoding:NSUTF8StringEncoding];
        PTDiffusionContent *const content =
            [[PTDiffusionContent alloc] initWithData:data];

        // Update the topic.
        [updater updateWithTopicPath:_TopicPath
                             content:content
                   completionHandler:^(NSError *const error)
        {
            if (error) {
                NSLog(@"Failed to update topic. Error: %@", error);
            } else {
                NSLog(@"Topic updated to \"%@\"", string);

                // Update topic after a short wait.
                [self updateTopicForSession:session withValue:value + 1];
            }
        }];
    });
}

@end
