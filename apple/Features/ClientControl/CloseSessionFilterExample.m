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

#import "CloseSessionFilterExample.h"

@import Diffusion;

@implementation CloseSessionFilterExample  {
    PTDiffusionSession* _normalSession1;
    PTDiffusionSession* _normalSession2;
    PTDiffusionSession* _controlSession;
    NSError *_error;
}

-(void)startWithURL:(NSURL*)url {

    /*
     * We'll run this example in a another thread other than main.
     * This allows to run operations in a synchronous way which is easier to read.
     */
    dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{

        // Creating normal session 1
        self->_normalSession1 = [self synchronousOpenSessionWithURL:url principal:@"client" andPassword:@"password"];
        if (!self->_normalSession1) {
            NSLog(@"Failed to open normal session 1: [%@]", self->_error);
            return;
        }
        NSLog(@"Created normal session 1 [%@]", self->_normalSession1.sessionId);


        // Creating normal session 2
        self->_normalSession2 = [self synchronousOpenSessionWithURL:url principal:@"client" andPassword:@"password"];
        if (!self->_normalSession2) {
            NSLog(@"Failed to open normal session 2: [%@]", self->_error);
            return;
        }
        NSLog(@"Created normal session 2 [%@]", self->_normalSession2.sessionId);


        // Creating control session
        self->_controlSession = [self synchronousOpenSessionWithURL:url principal:@"control" andPassword:@"password"];
        if (!self->_controlSession) {
            NSLog(@"Failed to open control session: [%@]", self->_error);
            return;
        }
        NSLog(@"Created control session [%@]", self->_controlSession.sessionId);


        // Adding session state listener for normal session 1
        NSNotificationCenter *const nc = [NSNotificationCenter defaultCenter];
        [nc addObserver:self
               selector:@selector(onSessionStateChangeNotification:)
                   name:PTDiffusionSessionStateDidChangeNotification
                 object:self->_normalSession1];


        // Setting country of normal session 1 to UK
        NSDictionary *sessionPropertiesNormalSession1 = @{PTDiffusionSession.countryPropertyKey : @"UK"};
        [self synchronousSetSessionProperties:sessionPropertiesNormalSession1
                                   forSession:self->_normalSession1
                          usingControlSession:self->_controlSession];


        // Setting country of normal session 2 to US
        NSDictionary *sessionPropertiesNormalSession2 = @{PTDiffusionSession.countryPropertyKey : @"US"};
        [self synchronousSetSessionProperties:sessionPropertiesNormalSession2
                                   forSession:self->_normalSession2
                          usingControlSession:self->_controlSession];


        // Closing sessions that have UK as their country
        NSString *sessionFilter = [NSString stringWithFormat:@"%@ EQ 'UK'", PTDiffusionSession.countryPropertyKey];
        [self->_controlSession.clientControl closeClientWithFilter:sessionFilter
                                          completionHandler:^(NSInteger closedSessions, NSError * _Nullable error)
         {
             if (error != nil) {
                 NSLog(@"An error occured while closing clients using session filter: [%@]", error);
             }
             else {
                 NSLog(@"Total clients closed: %ld", (long) closedSessions);
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


-(void)synchronousSetSessionProperties:(NSDictionary *)properties
                            forSession:(PTDiffusionSession *)session
                   usingControlSession:(PTDiffusionSession *)controlSession {

    dispatch_semaphore_t sema = dispatch_semaphore_create(0);
    [controlSession.clientControl setSessionProperties:properties
                                            forSession:session.sessionId
                                     completionHandler:^(PTDiffusionSetSessionPropertiesResult * _Nullable result, NSError * _Nullable error)
    {
        self->_error = error;
        dispatch_semaphore_signal(sema);
    }];
    dispatch_semaphore_wait(sema, DISPATCH_TIME_FOREVER);
}


-(void)onSessionStateChangeNotification:(NSNotification *const)notification {
    PTDiffusionSessionStateChange *const stateChange = notification.userInfo[PTDiffusionSessionStateChangeUserInfoKey];
    PTDiffusionSession *const session = (PTDiffusionSession *) notification.object;
    if (stateChange.state.isClosed && stateChange.state.error != nil) {
        NSLog(@"Session [%@] has been closed: [%@]", session.sessionId, stateChange.state.error);
    }
}

@end
