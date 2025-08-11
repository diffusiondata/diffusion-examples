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

#import "SessionTreesExample.h"

@import Diffusion;

@implementation SessionTreesExample  {
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


        // create a branch mapping table
        PTDiffusionBranchMappingTable *const table =
        [[[[[[PTDiffusionBranchMappingTableBuilder new]
             addBranchMappingWithSessionFilter:@"$Principal is 'control'" topicTreeBranch:@"content/control"]
            addBranchMappingWithSessionFilter:@"tier is '1'" topicTreeBranch:@"content/normal"]
           addBranchMappingWithSessionFilter:@"tier is '2'" topicTreeBranch:@"content/throttled"]
          addBranchMappingWithSessionFilter:@"all" topicTreeBranch:@"content/delayed"]
         createTableForSessionTreeBranch:@"public/content"];

        [session.sessionTrees putBranchMappingTable:table completionHandler:^(NSError * _Nullable error)
        {
            if (error != nil) {
                NSLog(@"An error occurred while setting the branch mapping table: %@", error.description);
                return;
            }
            NSLog(@"Branch mapping table has been set.");

            // get branch mapping table source branches
            [session.sessionTrees getSessionTreeBranchesWithMappings:^(NSArray<NSString *> * _Nullable sessionTreeBranches, NSError * _Nullable error)
             {
                if (error != nil) {
                    NSLog(@"An error occurred while retrieving the session tree branches: %@", error.description);
                    return;
                }

                for (NSString *sessionTreeBranch in sessionTreeBranches) {

                    // get branch mapping tree for source branch
                    [session.sessionTrees getBranchMappingTableForSessionTreeBranch:sessionTreeBranch
                                                             completionHandler:^(PTDiffusionBranchMappingTable * _Nullable branchMappingTable, NSError * _Nullable error)
                     {
                        if (error != nil) {
                            NSLog(@"An error occurred while retrieving the branch mapping table for '%@': %@", sessionTreeBranch, error.description);
                            return;
                        }
                        NSLog(@"Branch Mapping table for '%@'", branchMappingTable.sessionTreeBranch);
                        for (PTDiffusionBranchMapping *mapping in branchMappingTable.branchMappings) {
                            // format the table using padding for the session filter
                            NSLog(@"%*c%@   -->   %@", (int) (25 - mapping.sessionFilter.length), ' ', mapping.sessionFilter, mapping.topicTreeBranch);
                        }
                    }];
                }
            }];
        }];
    }];
}


@end
