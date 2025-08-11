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

#import "SetQueueEventHandlerExample.h"
#import "QueueEventListener.h"

@import Diffusion;

@implementation SetQueueEventHandlerExample  {
    PTDiffusionSession *_normalSession;
    PTDiffusionSession *_controlSession;
    QueueEventListener *_listener;
    PTDiffusionRegistration *_registration;
    NSError *_error;
}

-(void)startWithURL:(NSURL*)url {

    /*
     * We'll run this example in a another thread other than main.
     * This allows to run operations in a synchronous way which is easier to read.
     */
    dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{

        PTDiffusionMutableSessionConfiguration *smallQueueSessionConfiguration = [[PTDiffusionMutableSessionConfiguration alloc] init];
        smallQueueSessionConfiguration.principal = @"client";
        smallQueueSessionConfiguration.credentials = [[PTDiffusionCredentials alloc] initWithPassword:@"password"];

        // Creating normal session
        self->_normalSession = [self synchronousOpenSessionWithURL:url
                                                   andConfiguration:smallQueueSessionConfiguration];
        if (!self->_normalSession) {
            NSLog(@"Failed to open normal session: [%@]", self->_error);
            return;
        }
        NSLog(@"Created normal session [%@]", self->_normalSession.sessionId);


        // Creating control session
        PTDiffusionMutableSessionConfiguration *controlSessionConfiguration = [[PTDiffusionMutableSessionConfiguration alloc] init];
        controlSessionConfiguration.principal = @"control";
        controlSessionConfiguration.credentials = [[PTDiffusionCredentials alloc] initWithPassword:@"password"];

        self->_controlSession = [self synchronousOpenSessionWithURL:url
                                                   andConfiguration:controlSessionConfiguration];
        if (!self->_controlSession) {
            NSLog(@"Failed to open control session: [%@]", self->_error);
            return;
        }
        NSLog(@"Created control session [%@]", self->_controlSession.sessionId);

        // set QueueEventListener
        self->_listener = [[QueueEventListener alloc] init];
        [self->_controlSession.clientControl setQueueEventHandler:self->_listener
                                                completionHandler:^(PTDiffusionRegistration * _Nonnull registration, NSError * _Nullable error)
         {
            if (registration == nil) {
                NSLog(@"An error occurred while registering the QueueEventListener: %@", error.description);
                return;
            }

            self->_registration = registration;
            NSLog(@"QueueEventListener successfully registered!");
        }];

    });
}


-(PTDiffusionSession *)synchronousOpenSessionWithURL:(NSURL *)url
                                    andConfiguration:(PTDiffusionSessionConfiguration *)configuration {

    dispatch_semaphore_t sema = dispatch_semaphore_create(0);

    __block PTDiffusionSession *newSession;
    [PTDiffusionSession openWithURL:url
                      configuration:configuration
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
