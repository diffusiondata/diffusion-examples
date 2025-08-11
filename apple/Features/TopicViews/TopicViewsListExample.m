//  Diffusion Client Library for iOS, tvOS and OS X / macOS - Examples
//
//  Copyright (C) 2020 - 2023 DiffusionData Ltd.
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

#import "TopicViewsListExample.h"

@import Diffusion;

@implementation TopicViewsListExample  {
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

         // create multiple topics and populate them
         [self createAndPopulateTopicPath:@"topic/example1" withValue:@"hello world!" usingSession:session];
         [self createAndPopulateTopicPath:@"topic/example2" withValue:@"push technology" usingSession:session];
         [self createAndPopulateTopicPath:@"topic/example3" withValue:@"diffusion" usingSession:session];

         // create a topic view for each topic created
         [self createTopicViewNamed:@"view 1" forTopicPath:@"topic/example1" usingSession:session];
         [self createTopicViewNamed:@"view 2" forTopicPath:@"topic/example2" usingSession:session];
         [self createTopicViewNamed:@"view 3" forTopicPath:@"topic/example3" usingSession:session];

         // list topic views
         [self listTopicViewsUsingSession:session];
     }];
}


-(void) createAndPopulateTopicPath:(NSString *const)topicPath
                         withValue:(NSString *const)value
                      usingSession:(PTDiffusionSession *const)session {


    PTDiffusionTopicSpecification *const specification =
        [[PTDiffusionTopicSpecification alloc] initWithType:PTDiffusionTopicType_String];

    [session.topicControl addTopicWithPath:topicPath
                             specification:specification
                         completionHandler:^(PTDiffusionAddTopicResult * _Nullable result, NSError * _Nullable error)
     {
         if ([result isEqual:PTDiffusionAddTopicResult.exists]) {
             NSLog(@"Topic already existed");
         }
         else if ([result isEqual:PTDiffusionAddTopicResult.created]) {
             NSLog(@"Topic created");
         }
         else if (error != nil) {
             NSLog(@"An error occurred while attempting to create topic: [%@]", error);
             return;
         }

         id completionHandler = ^(NSError * _Nullable error) {
             if (error != nil) {
                 NSLog(@"An error occurred while attempting to set the value to the topic: [%@]", error);
             }
         };

         NSError *updateError;
         [session.topicUpdate setWithPath:topicPath
                            toStringValue:value
                        completionHandler:completionHandler
                                    error:&updateError];
     }];
}


-(void)createTopicViewNamed:(NSString *const)name
               forTopicPath:(NSString *const)topicPath
               usingSession:(PTDiffusionSession *const)session {

    NSString *const specification =
        [NSString stringWithFormat:@"map ?%@ to views/<path(0)>", topicPath];

    [session.topicViews createTopicViewWithName:name
                                  specification:specification
                              completionHandler:^(PTDiffusionTopicView * _Nullable view, NSError * _Nullable error)
     {
         if (error != nil) {
             NSLog(@"An error occured while creating the topic view: [%@]", error);
             return;
         }

         NSString *const name = view.name;
         NSArray<NSString *> *const roles = view.roles;

         NSLog(@"Topic View [%@] was successfully created with the following roles: [%@]", name, roles);
     }];
}


-(void)listTopicViewsUsingSession:(PTDiffusionSession *const)session {
    [session.topicViews listTopicViewsWithCompletionHandler:^(NSArray<PTDiffusionTopicView *> * _Nullable views, NSError * _Nullable error) {
        if (error != nil) {
            NSLog(@"An error occured while listing the topic views: [%@]", error);
            return;
        }

        for (PTDiffusionTopicView *view in views) {
            NSLog(@"TopicView [%@] using specification [%@] and with roles [%@]", view.name, view.specification, view.roles);
        }
    }];
}

@end
