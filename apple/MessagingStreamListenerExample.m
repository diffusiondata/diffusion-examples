//  Diffusion Client Library for iOS and OS X - Examples
//
//  Copyright (C) 2015, 2016 Push Technology Ltd.
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

#import "MessagingStreamListenerExample.h"

@import Diffusion;

@interface MessagingStreamListenerExample (PTDiffusionMessageStreamDelegate) <PTDiffusionMessageStreamDelegate>
@end

@implementation MessagingStreamListenerExample {
    PTDiffusionSession* _session;
}

-(void)startWithURL:(NSURL*)url
 sessionConfiguration:(PTDiffusionSessionConfiguration*)sessionConfiguration {
    NSLog(@"Connecting...");

    [PTDiffusionSession openWithURL:url
                      configuration:sessionConfiguration
                  completionHandler:^(PTDiffusionSession *session, NSError *error)
     {
         if (!session) {
             NSLog(@"Failed to open session: %@", error);
             return;
         }

         // At this point we now have a connected session.
         NSLog(@"Connected.");

         // Set ivar to maintain a strong reference to the session.
         _session = session;

         // Register as delegate to handle incoming messages.
         [session.messaging addFallbackMessageStreamWithDelegate:self];
     }];
}

@end

@implementation MessagingStreamListenerExample (PTDiffusionMessageStreamDelegate)

-(void)      diffusionStream:(PTDiffusionStream *const)stream
didReceiveMessageOnTopicPath:(NSString *const)topicPath
                     content:(PTDiffusionContent *const)content
                     context:(PTDiffusionReceiveContext *const)context {
    NSLog(@"Received on \"%@\": %@ (context: %@)", topicPath, content, context);
}

@end
