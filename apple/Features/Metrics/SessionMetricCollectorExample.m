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

#import "SessionMetricCollectorExample.h"

@import Diffusion;

@implementation SessionMetricCollectorExample  {
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

        // create a session metric collector using its builder
        PTDiffusionSessionMetricCollector *const collector =
            [[[[[[PTDiffusionSessionMetricCollectorBuilder new]
                 exportToPrometheus:YES]
                maximumGroups:10]
               removeMetricsWithNoMatches:NO]
              groupByProperty:@"$Location"]
             createCollectorWithName:@"collector 1" andSessionFilter:@"$Principal is 'control'"];

        PTDiffusionMetricsFeature *const metrics = session.metrics;

        // place the session metric collector in the server
        [metrics putSessionMetricCollector:collector
                         completionHandler:^(NSError * _Nullable error)
        {
            if (error != nil) {
                NSLog(@"An error has occurred while setting the collector: %@", error);
                return;
            }

            NSLog(@"Listing available session metric collectors");

            // list all session metric collectors in the server
            [metrics listSessionMetricCollectors:^(NSArray<PTDiffusionSessionMetricCollector *> * _Nullable collectors,
                                                   NSError * _Nullable error)
             {
                if (error != nil) {
                    NSLog(@"An error has occurred while listing the available session metric collectors: %@", error);
                    return;
                }

                for (PTDiffusionSessionMetricCollector *c in collectors) {
                    NSLog(@"%@", c.name);
                    NSLog(@"\tSession Filter: %@", c.sessionFilter);
                    NSLog(@"\tExports to Prometheus: %@", c.exportsToPrometheus ? @"YES" : @"NO");
                    NSLog(@"\tMaximum groups: %d", (int) c.maximumGroups);
                    NSLog(@"\tRemoves metrics with no matches: %@", c.removesMetricsWithNoMatches ? @"YES" : @"NO");
                    NSLog(@"\tGroup by properties:");
                    for (NSString *property in c.groupByProperties) {
                        NSLog(@"\t\t%@", property);
                    }
                }

                NSLog(@"Removing collector");

                // remove the session metric collector we created
                [metrics removeSessionMetricCollector:@"collector 1"
                                    completionHandler:^(NSError * _Nullable error)
                {
                    if (error != nil) {
                        NSLog(@"An error has occurred while removing the session metric collector: %@", error);
                        return;
                    }

                    NSLog(@"Done.");
                }];
            }];

        }];
    }];
}


@end
