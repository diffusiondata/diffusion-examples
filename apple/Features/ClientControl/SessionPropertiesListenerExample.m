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

#import "SessionPropertiesListenerExample.h"

@import Diffusion;

@interface SessionPropertiesListenerExample (PTDiffusionSessionPropertiesDelegate) <PTDiffusionSessionPropertiesDelegate>
@end

@implementation SessionPropertiesListenerExample  {
    PTDiffusionSession* _normalSession1;
    PTDiffusionSession* _normalSession2;
    PTDiffusionSession* _controlSession;
    PTDiffusionSessionPropertiesListenerRegistration *_listenerRegistration;
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


        // Adding the session properties listener in the control session
        // This will trigger 3 events of diffusionSessionPropertiesListenerRegistration:sessionOpened:withProperties:
        // One for each session we created in this example.
        NSArray *requestedSessionProperties = @[PTDiffusionSession.allUserProperties, PTDiffusionSession.allFixedProperties];
        [self->_controlSession.clientControl addSessionPropertiesListener:self
                                                            forProperties:requestedSessionProperties
                                                        completionHandler:^(PTDiffusionSessionPropertiesListenerRegistration * _Nullable registration,
                                                                            NSError * _Nullable error)
         {
             if (error != nil) {
                 NSLog(@"An error occurred while adding the session properties listener: [%@]", error);
                 return;
             }
             else
             {
                 NSLog(@"Session properties listener has been registered");
                 self->_listenerRegistration = registration;
             }

             // Running the rest of the operations in another thread.
             // This allows to run operations in a synchronous way which is easier to read.
             dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{

                 [NSThread sleepForTimeInterval:2.0];

                 // Setting country of normal session 1 to UK
                 // This will trigger an event of diffusionSessionPropertiesListenerRegistration:sessionUpdated:withProperties:
                 NSDictionary *sessionPropertiesNormalSession1 = @{PTDiffusionSession.countryPropertyKey : @"UK"};
                 [self synchronousSetSessionProperties:sessionPropertiesNormalSession1
                                            forSession:self->_normalSession1
                                   usingControlSession:self->_controlSession];

                 [NSThread sleepForTimeInterval:2.0];

                 // Setting country of normal session 2 to US
                 // This will trigger and event of diffusionSessionPropertiesListenerRegistration:sessionUpdated:withProperties:
                 NSDictionary *sessionPropertiesNormalSession2 = @{PTDiffusionSession.countryPropertyKey : @"US"};
                 [self synchronousSetSessionProperties:sessionPropertiesNormalSession2
                                            forSession:self->_normalSession2
                                   usingControlSession:self->_controlSession];

                 [NSThread sleepForTimeInterval:2.0];

                 // Closing normal session 2
                 // This will trigger an event of diffusionSessionPropertiesListenerRegistration:sessionClosed:withCloseReason:andProperties:
                 // The close reason will be ClosedByController
                 [self synchronousCloseSession:self->_normalSession2
                           usingControlSession:self->_controlSession];

                 [NSThread sleepForTimeInterval:2.0];

                 // Removing Session Properties Listener
                 // This will trigger an event diffusionSessionPropertiesListenerRegistrationDidClose:
                 [self->_listenerRegistration closeWithCompletionHandler:^(NSError * _Nullable error) {
                     if (error != nil) {
                         NSLog(@"An error occurred while closing the session properties listener: [%@]", error);
                     }
                     else {
                         NSLog(@"Session Properties Listener has been closed.");
                     }
                 }];
             });
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


-(void) synchronousCloseSession:(PTDiffusionSession *)session
            usingControlSession:(PTDiffusionSession *)controlSession {

    dispatch_semaphore_t sema = dispatch_semaphore_create(0);
    [controlSession.clientControl closeClientWithSessionId:session.sessionId
                                         completionHandler:^(NSError * _Nullable error)
      {
          self->_error = error;
          dispatch_semaphore_signal(sema);
      }];
    dispatch_semaphore_wait(sema, DISPATCH_TIME_FOREVER);
}


-(void)printProperties:(NSDictionary<NSString *, id> *)properties {
    for (NSString *key in properties.allKeys) {
        NSLog(@" - %@: [%@]", key, ([properties[key] isEqual:NSNull.null]) ? @"nil" : properties[key]);
    }
    NSLog(@" --- ");
}



#pragma mark - PTDiffusionSessionPropertiesDelegate methods


-(void)diffusionSessionPropertiesListenerRegistration:(PTDiffusionSessionPropertiesListenerRegistration *)registration
                                        sessionOpened:(PTDiffusionSessionId *)sessionId
                                       withProperties:(NSDictionary<NSString *, id>*)properties {
    NSLog(@"Session Opened [%@]", sessionId);
    [self printProperties:properties];
}


-(void)diffusionSessionPropertiesListenerRegistration:(PTDiffusionSessionPropertiesListenerRegistration *)registration
                                        sessionClosed:(PTDiffusionSessionId *)sessionId
                                      withCloseReason:(PTDiffusionCloseReason *) closeReason
                                        andProperties:(NSDictionary<NSString *, id>*)properties {
    NSLog(@"Session Closed [%@]: [%@]", sessionId, closeReason);
    [self printProperties:properties];
}


-(void)diffusionSessionPropertiesListenerRegistration:(PTDiffusionSessionPropertiesListenerRegistration *)registration
                                       sessionUpdated:(PTDiffusionSessionId *)sessionId
                                       withProperties:(NSDictionary<NSString *, id>*)properties {
    NSLog(@"Session Updated [%@]", sessionId);
    [self printProperties:properties];
}


-(void)diffusionSessionPropertiesListenerRegistration:(PTDiffusionSessionPropertiesListenerRegistration *)registration
                                  sessionDisconnected:(PTDiffusionSessionId *)sessionId
                                       withProperties:(NSDictionary<NSString *, id>*)properties {
    NSLog(@"Session Disconnected [%@]", sessionId);
    [self printProperties:properties];
}


-(void)diffusionSessionPropertiesListenerRegistration:(PTDiffusionSessionPropertiesListenerRegistration *)registration
                                   sessionReconnected:(PTDiffusionSessionId *)sessionId
                                       withProperties:(NSDictionary<NSString *, id>*)properties {
    NSLog(@"Session Reconnected [%@]", sessionId);
    [self printProperties:properties];
}


-(void)diffusionSessionPropertiesListenerRegistration:(PTDiffusionSessionPropertiesListenerRegistration *)registration
                                    sessionFailedOver:(PTDiffusionSessionId *)sessionId
                                       withProperties:(NSDictionary<NSString *, id>*)properties {
    NSLog(@"Session Failed Over [%@]", sessionId);
    [self printProperties:properties];
}



#pragma mark - PTDiffusionSessionPropertiesListenerRegistrationDelegate methods


-(void)diffusionSessionPropertiesListenerRegistrationDidClose:(PTDiffusionSessionPropertiesListenerRegistration *)registration {

    NSLog(@"Session Properties Listener Registration is now closed.");
}


-(void)diffusionSessionPropertiesListenerRegistration:(PTDiffusionSessionPropertiesListenerRegistration *)registration
                                     didFailWithError:(NSError *)error {

    NSLog(@"Session Properties Listener Registration failed with error: [%@]", error);
}

@end
