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

#import "SetQueueConflationExample.h"

@import Diffusion;

@implementation SetQueueConflationExample  {
    PTDiffusionSession* _normalSession;
    PTDiffusionSession* _controlSession;
    NSError *_error;
}

-(void)startWithURL:(NSURL*)url {

    /*
     * We'll run this example in a another thread other than main.
     * This allows to run operations in a synchronous way which is easier to read.
     */
    dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{

        // Creating normal session
        self->_normalSession = [self synchronousOpenSessionWithURL:url principal:@"client" andPassword:@"password"];
        if (!self->_normalSession) {
            NSLog(@"Failed to open normal session: [%@]", self->_error);
            return;
        }
        NSLog(@"Created normal session [%@]", self->_normalSession.sessionId);


        // Creating control session
        self->_controlSession = [self synchronousOpenSessionWithURL:url principal:@"control" andPassword:@"password"];
        if (!self->_controlSession) {
            NSLog(@"Failed to open control session: [%@]", self->_error);
            return;
        }
        NSLog(@"Created control session [%@]", self->_controlSession.sessionId);


        // Enabling the normal session's queue conflation
        [self->_controlSession.clientControl setQueueConflation:YES
                                                     forSession:self->_normalSession.sessionId
                                              completionHandler:^(NSError * _Nullable error)
        {
            if (error != nil) {
                NSLog(@"An error has occurred while enabling the normal session's queue conflation: [%@]", error);
            }
            else {
                NSLog(@"Normal session's queue conflation has been enabled.");
            }
        }];
    });
}


-(PTDiffusionSession *)synchronousOpenSessionWithURL:(NSURL *)url
                                           principal:(NSString *)principal
                                         andPassword:(NSString *)password {

    PTDiffusionCredentials *const credentials = [[PTDiffusionCredentials alloc] initWithPassword:password];

    PTDiffusionSessionConfiguration *const sessionConfiguration =
        [[PTDiffusionSessionConfiguration alloc] initWithPrincipal:principal
                                                       credentials:credentials];

    dispatch_semaphore_t sema = dispatch_semaphore_create(0);

    __block PTDiffusionSession *newSession;
    [PTDiffusionSession openWithURL:url
                      configuration:sessionConfiguration
                  completionHandler:^(PTDiffusionSession * _Nullable session, NSError * _Nullable error)
     {
         newSession = session;
         self->_error = error;
         dispatch_semaphore_signal(sema);
     }];
    dispatch_semaphore_wait(sema, DISPATCH_TIME_FOREVER);
    return newSession;
}


@end
