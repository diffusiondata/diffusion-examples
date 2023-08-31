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

#import "PingExample.h"

@import Diffusion;

@implementation PingExample {
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
         NSLog(@"Connected. Session Identifier: %@", session.sessionId);
         
         // Set ivar to maintain a strong reference to the session.
         self->_session = session;
         
         [self->_session.pings pingServerWithCompletionHandler:^(PTDiffusionPingDetails * _Nullable details, NSError * _Nullable error) {
             if (error) {
                 NSLog(@"Failed to ping the server: %@", error);
                 return;
             }
             
             NSLog(@"Server Ping successful with %dms latency", (int) round(details.roundTripTime * 1000));
         }];
     }];
}

@end
