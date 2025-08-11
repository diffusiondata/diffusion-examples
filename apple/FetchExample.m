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

#import "FetchExample.h"

@import Diffusion;

/**
 Example of fetch without values.
 */
@implementation FetchExample {
    PTDiffusionSession* _session;
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
        self->_session = session;

        // Send fetch request.
        [[session.topics fetchRequest] fetchWithTopicSelectorExpression:@"*Assets//"
                                                      completionHandler:
        ^(PTDiffusionFetchResult* result, NSError* error)
        {
            if (result) {
                for (PTDiffusionFetchTopicResult* topicResult in result.results) {
                    NSLog(@"Fetch topic result, \"%@\": %@",
                        topicResult.path,
                        topicResult.specification);
                }
            } else {
                NSLog(@"Fetch failed with error: %@", error);
            }
        }];

        // perform the same operation as above but use limitDeepbranches.
        // A deep branch has a root path that has a
        // number of parts equal to the deep_branch_depth parameter. 
        // The deep_branch_limit specifies the maximum number of results for each deep branch.
        PTDiffusionFetchRequest *const request =
            [session.topics.fetchRequest limitDeepBranches:3
                                                     limit:3];

            [request fetchBinaryValuesWithTopicSelectorExpression:@"?.//"
                                                completionHandler:
            ^(PTDiffusionBinaryFetchResult *const result, NSError *const error)
            {
            if (result) {
                for (PTDiffusionFetchTopicResult* topicResult in result.results) {
                    NSLog(@"Fetch topic result, \"%@\": %@",
                        topicResult.path,
                        topicResult.specification);
                }
            } else {
                NSLog(@"Fetch failed with error: %@", error);
            }
        }];

        
        
    }];
}

@end
