//  Diffusion Client Library for iOS, tvOS and OS X / macOS - Examples
//
//  Copyright (C) 2021 - 2023 DiffusionData Ltd.
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

#import "TopicMetricCollectorExample.h"

@import Diffusion;

@implementation TopicMetricCollectorExample  {
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

        // create a topic metric collector using its builder
        PTDiffusionTopicMetricCollector *const collector =
            [[[[[[PTDiffusionTopicMetricCollectorBuilder new]
                 exportToPrometheus:NO]
                maximumGroups:10]
               groupByTopicType:YES]
              groupByTopicView:YES]
             createCollectorWithName:@"collector 1" andTopicSelector:@"*A/B/C//"];

        PTDiffusionMetricsFeature *const metrics = session.metrics;

        // place the topic metric collector in the server
        [metrics putTopicMetricCollector:collector
                       completionHandler:^(NSError * _Nullable error)
        {
            if (error != nil) {
                NSLog(@"An error has occurred while setting the collector: %@", error);
                return;
            }

            NSLog(@"Listing available topic metric collectors");

            // list all topic metric collectors in the server
            [metrics listTopicMetricCollectors:^(NSArray<PTDiffusionTopicMetricCollector *> * _Nullable collectors,
                                                 NSError * _Nullable error)
             {
                if (error != nil) {
                    NSLog(@"An error has occurred while listing the available topic metric collectors: %@", error);
                    return;
                }

                for (PTDiffusionTopicMetricCollector *c in collectors) {
                    NSLog(@"%@", c.name);
                    NSLog(@"\tTopic Selector: %@", c.topicSelector);
                    NSLog(@"\tExports to Prometheus: %@", c.exportsToPrometheus ? @"YES" : @"NO");
                    NSLog(@"\tGroups by Topic type: %@", c.groupsByTopicType ? @"YES" : @"NO");
                    NSLog(@"\tMaximum groups: %d", (int) c.maximumGroups);
                }

                NSLog(@"Removing topic metric collector");

                // remove the topic metric collector we created
                [metrics removeTopicMetricCollector:@"collector 1"
                                  completionHandler:^(NSError * _Nullable error)
                {
                    if (error != nil) {
                        NSLog(@"An error has occurred while removing the topic metric collector: %@", error);
                        return;
                    }

                    NSLog(@"Done.");
                }];
            }];

        }];
    }];
}


@end
