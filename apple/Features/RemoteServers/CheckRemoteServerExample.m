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

#import "CheckRemoteServerExample.h"

@import Diffusion;

@implementation CheckRemoteServerExample  {
    PTDiffusionSession* _session;
}

-(void)startWithURL:(NSURL*)url {

    PTDiffusionCredentials *const credentials =
        [[PTDiffusionCredentials alloc] initWithPassword:@"password"];

    // To add remote servers, you will need an admin principal or CONTROL_SERVER permission
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

         // these are the principal and credentials of the remote server to be addded
         NSString *const remoteServerPrincipal = @"principal";
         PTDiffusionCredentials *const remoteServerCredentials = PTDiffusionCredentials.noCredentials;

         NSString *const serverName = @"New remote server";

        PTDiffusionSecondaryInitiatorRemoteServer *const remoteServer =
            [[[[PTDiffusionRemoteServerBuilder new]
               principal:remoteServerPrincipal]
              credentials:remoteServerCredentials]
             createSecondaryInitiatorWithName:serverName andURL:@"ws://new.server.url.com"];

         // creating a remote server with default Connection Options
         [session.remoteServers createRemoteServer:remoteServer
                                 completionHandler:^(PTDiffusionCreateRemoteServerResult * _Nullable result,
                                                     NSError * _Nullable error)
          {
              if (result == nil) {
                  NSLog(@"Remote Server was not added. [%@]", error.description);
                  return;
              }

              if (!result.success) {
                  NSLog(@"Remote Server was not added. Errors:");
                  for (NSError *error in result.errors) {
                      NSLog(@"%@", error.description);
                  }
                  return;
              }

              NSLog(@"Remote Server [%@] has been successfully added!", result.remoteServer);

              // check Remote Server
              // Please Note: The server requires REMOTE_CONNECTIONS feature in the license to establish a connection to the remote server.
              [session.remoteServers checkRemoteServer:serverName
                                     completionHandler:^(PTDiffusionCheckRemoteServerResult * _Nullable result, NSError * _Nullable error)
              {
                  if (result == nil && error != nil) {
                      NSLog(@"An error occurred while retrieving the status of remote server [%@]: [%@]", serverName, error.description);
                      return;
                  }

                  if ([result.state isEqual:PTDiffusionRemoteServerConnectionState.inactive]) {
                      NSLog(@"The Remote Server is inactive.");
                  }
                  else if ([result.state isEqual:PTDiffusionRemoteServerConnectionState.connected]) {
                      NSLog(@"The Remote Server is connected.");
                  }
                  else if ([result.state isEqual:PTDiffusionRemoteServerConnectionState.retrying]) {
                      NSLog(@"Connection to the Remote Server has failed but is scheduled to try again: %@", result.failureMessage);
                  }
                  else if ([result.state isEqual:PTDiffusionRemoteServerConnectionState.failed]) {
                      NSLog(@"Connection to the Remote Server has failed: %@", result.failureMessage);
                  }
                  else if ([result.state isEqual:PTDiffusionRemoteServerConnectionState.missing]) {
                      NSLog(@"Unable to reach Remote Server.");
                  }
              }];
          }];
     }];
}


@end
