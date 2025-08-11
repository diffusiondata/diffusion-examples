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

#import "MessagingRequestResponseExample.h"

@import Diffusion;

@interface MessagingRequestResponseExample (PTDiffusionStringSessionResponseStreamDelegate) <PTDiffusionStringSessionResponseStreamDelegate, PTDiffusionStringRequestDelegate>
@end

@implementation MessagingRequestResponseExample  {
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

         // setup the message request handler (Receiver)
         [self setupMessageRequestHandlerForSession:session];

         // send the message (Sender)
         [self sendRequestToFilterForSession:session];
     }];
}


-(void) setupMessageRequestHandlerForSession:(PTDiffusionSession *const) session {

    PTDiffusionRequestHandler *const handler = [PTDiffusionPrimitive stringRequestHandlerWithDelegate:self];

    [session.messaging addRequestHandler:handler
                                 forPath:@"account/offers"
                       completionHandler:^(PTDiffusionTopicTreeRegistration * _Nullable registration, NSError * _Nullable error)
    {
        if (registration != nil && error == nil) {
            NSLog(@"Message Request Handler has been registered [%@]", registration);
        }
        else {
            NSLog(@"An error has occurred while attempting to register the Message Request Handler: [%@]", error);
        }
    }];
}


-(void) sendRequestToFilterForSession:(PTDiffusionSession *const) session {
    NSError *error;

    PTDiffusionRequest *const request =
        [PTDiffusionPrimitive requestWithString:@"Special offer!" error:&error];

    PTDiffusionSessionResponseStream *const responseStream =
        [PTDiffusionPrimitive stringSessionResponseStreamWithDelegate:self];

    [session.messaging sendRequest:request
                          toFilter:@"$Principal is 'alice'"
                              path:@"account/offers"
                    responseStream:responseStream
                 completionHandler:^(NSUInteger count, NSError * _Nullable error)
    {
        NSLog(@"Received response from %d total sessions", (int) count);
    }];
}


#pragma mark - PTDiffusionStringRequestDelegate

- (void)diffusionTopicTreeRegistration:(nonnull PTDiffusionTopicTreeRegistration *)registration didFailWithError:(nonnull NSError *)error {
    NSLog(@"Registration [%@] failed with error [%@]", registration, error);
}

- (void)diffusionTopicTreeRegistrationDidClose:(nonnull PTDiffusionTopicTreeRegistration *)registration {
    NSLog(@"Registration [%@] is now closed", registration);
}

- (void)diffusionTopicTreeRegistration:(nonnull PTDiffusionTopicTreeRegistration *)registration
           didReceiveRequestWithString:(nullable NSString *)string
                               context:(nonnull PTDiffusionRequestContext *)context
                             responder:(nonnull PTDiffusionResponder *)responder {
    NSLog(@"Registration [%@] received request with string [%@]", registration, string);

    NSError *error;
    [responder respondWithString:@"Tell me more" error:&error];
}



#pragma mark - PTDiffusionStringSessionResponseStreamDelegate methods


-(void)          diffusionStream:(PTDiffusionStream *)stream
    didReceiveResponseWithString:(nullable NSString *)string
                   fromSessionId:(PTDiffusionSessionId *)sessionId {
    NSLog(@"Stream [%@] received response with string [%@] from session [%@]", stream, string, sessionId);
}

- (void)diffusionStream:(nonnull PTDiffusionStream *)stream
        didReceiveError:(nonnull NSError *)error
          fromSessionId:(nonnull PTDiffusionSessionId *)sessionId {
    NSLog(@"Stream [%@] received error [%@] from session [%@]", stream, error, sessionId);
}

@end
