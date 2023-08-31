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

#import "CreateRemoteServerExample.h"

@import Diffusion;

@implementation CreateRemoteServerExample  {
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
        NSString *remoteServerPrincipal = @"principal";
        PTDiffusionCredentials *remoteServerCredentials = PTDiffusionCredentials.noCredentials;

        // use the builder to create the remote server object
        PTDiffusionSecondaryInitiatorRemoteServer *const remoteServer =
            [[[[PTDiffusionRemoteServerBuilder new]
               principal:remoteServerPrincipal]
              credentials:remoteServerCredentials]
             createSecondaryInitiatorWithName:@"New remote server" andURL:@"ws://new.server.url.com"];

        // creating a remote server with default Connection Options
        [session.remoteServers createRemoteServer:remoteServer
                                completionHandler:^(PTDiffusionCreateRemoteServerResult * _Nullable result,
                                                    NSError * _Nullable error)
         {
            if (result != nil) {
                if (result.success) {
                    NSLog(@"Remote Server [%@] has been successfully added!", result.remoteServer);
                }
                else {
                    NSLog(@"Remote Server was not added. Errors:");
                    for (NSError *error in result.errors) {
                        NSLog(@"%@", error.description);
                    }
                }
            }
            else {
                NSLog(@"Remote Server was not added. [%@]", error.description);
            }


            /*
             Creating a second remote server with the following connection options:
             - 120s for reconnection timeout
             - 1000 messages for maximum queue size
             - 15s for connection timeout
             */
            NSDictionary *connectionOptions = @{PTDiffusionRemoteServerConnectionOption.reconnectionTimeout: @"120000",
                                                PTDiffusionRemoteServerConnectionOption.maximumQueueSize: @"1000",
                                                PTDiffusionRemoteServerConnectionOption.connectionTimeout: @"15000"
            };

            PTDiffusionSecondaryInitiatorRemoteServer *const remoteServer_2 =
                [[[[[PTDiffusionRemoteServerBuilder new]
                    principal:remoteServerPrincipal]
                   credentials:remoteServerCredentials]
                  connectionOptions:connectionOptions]
                 createSecondaryInitiatorWithName:@"New remote server 2" andURL:@"ws://another.server.url.com"];

            [session.remoteServers createRemoteServer:remoteServer_2
                                    completionHandler:^(PTDiffusionCreateRemoteServerResult * _Nullable result,
                                                        NSError * _Nullable error)
             {
                if (result != nil) {
                    if (result.success) {
                        NSLog(@"Remote Server [%@] has been successfully added!", result.remoteServer);
                    }
                    else {
                        NSLog(@"Remote Server was not added. Errors:");
                        for (NSError *error in result.errors) {
                            NSLog(@"%@", error.description);
                        }
                    }
                }
                else {
                    NSLog(@"Remote Server was not added. [%@]", error.description);
                }
            }];
        }];
    }];
}


@end
