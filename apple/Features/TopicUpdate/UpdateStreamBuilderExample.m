//  Diffusion Client Library for iOS, tvOS and OS X / macOS - Examples
//
//  Copyright (C) 2022 - 2023 DiffusionData Ltd.
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

#import "UpdateStreamBuilderExample.h"

@import Diffusion;


@implementation UpdateStreamBuilderExample{
    PTDiffusionSession* _session;
}

-(void)startWithURL:(NSURL*)url {

    PTDiffusionCredentials *const credentials =
        [[PTDiffusionCredentials alloc] initWithPassword:@"password"];

    PTDiffusionSessionConfiguration *const sessionConfiguration =
        [[PTDiffusionSessionConfiguration alloc] initWithPrincipal:@"admin"
                                                       credentials:credentials];

    NSLog(@"Connecting...");

    [PTDiffusionSession openWithURL:url
                      configuration:sessionConfiguration
                  completionHandler:^(PTDiffusionSession *const session, NSError *const error)
     {
        if (!session) {
            NSLog(@"Failed to open session: %@", error);
            return;
        }

        // At this point we now have a connected session.
        NSLog(@"Connected.");

        // Set ivar to maintain a strong reference to the session.
        self->_session = session;

        // create an update stream builder
        PTDiffusionUpdateStreamBuilder *const builder = [session.topicUpdate newUpdateStreamBuilder];

        // create the Topic Specification and set the Update Stream Builder to it
        PTDiffusionTopicSpecification * const specification = [[PTDiffusionTopicSpecification alloc] initWithType:PTDiffusionTopicType_JSON];
        [builder topicSpecification:specification];

        // create an Update Stream
        PTDiffusionJSONUpdateStream *const updateStream = [builder jsonUpdateStreamWithPath:@"a/b/c"];

        // use the update stream to create and set the topic value
        PTDiffusionJSON *value = [[PTDiffusionJSON alloc] initWithJSONString:@"{\"hello\": \"world\"}" error:nil];
        [updateStream setValue:value
             completionHandler:^(PTDiffusionTopicCreationResult * _Nullable result, NSError * _Nullable error) {
            if (result != nil) {
                NSLog(@"Topic has been updated!");
            }
        }
                         error:nil];
    }];
}

@end
