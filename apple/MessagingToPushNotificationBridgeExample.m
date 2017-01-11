//  Diffusion Client Library for iOS and OS X - Examples
//
//  Copyright (C) 2016 Push Technology Ltd.
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

//** The default path at which the Push Notification Bridge listens for messaging
#define SERVICE_TOPIC @"push/notifications"

#import "MessagingToPushNotificationBridgeExample.h"

@import Diffusion;

@implementation MessagingToPushNotificationBridgeExample {
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
         NSLog(@"Connected.");

         // Set ivar to maintain a strong reference to the session.
         _session = session;

         // An example APNs device token
         unsigned char tokenBytes[] =
            {0x5a, 0x88, 0x3a, 0x57, 0xe2, 0x89, 0x77, 0x84,
             0x1d, 0xc8, 0x1a, 0x0a, 0xa1, 0x4e, 0x2f, 0xdf,
             0x64, 0xc6, 0x5a, 0x8f, 0x7b, 0xb1, 0x9a, 0xa1,
             0x6e, 0xaf, 0xc3, 0x16, 0x13, 0x18, 0x1c, 0x97};
         NSData *const deviceToken = [NSData dataWithBytes:(void *)tokenBytes length:32];

         [self doPnSubscribe:@[@"some/topic/name"] deviceToken:deviceToken];
     }];
}

/**
 * Compose a URI understood by the Push Notification Bridge from an APNs device token.
 * @param deviceID APNS device token.
 * @return string in format expected by the push notification bridge.
 */
-(NSString*)formatAsURI:(NSData*)deviceID {
    return [NSString stringWithFormat:@"apns://%@", [deviceID base64EncodedStringWithOptions:0]];
}

/**
 * Compose and send a subscription request to the Push Notification bridge
 * @param paths topic paths within the subscription request
 */
- (void)doPnSubscribe:(NSArray<NSString*> *)paths deviceToken:(NSData*)deviceToken {
    // Compose the JSON request from Obj-C literals
    NSString *const correlation = [[NSUUID UUID] UUIDString];
    PTDiffusionTopicSelector *const selector = [PTDiffusionTopicSelector topicSelectorWithAnyExpression:paths];
    NSDictionary *const request = 
        @{@"request": @{
           @"correlation": correlation,
           @"content": @{
              @"pnsub": @{
                @"destination": [self formatAsURI:deviceToken],
                @"topic": selector.description}
              }
           }};
    NSData *const requestData = [NSJSONSerialization dataWithJSONObject:request options:0 error:nil];

    // Send a message to `SERVICE_TOPIC`
    [_session.messaging sendWithTopicPath:SERVICE_TOPIC
                                    value:[[PTDiffusionContent alloc] initWithData:requestData]
                        completionHandler:^(NSError * _Nullable error)
                        {
                                if(error != nil) {
                                    NSLog(@"Send to topic %@ failed: %@", SERVICE_TOPIC, error);
                                }
                        }];
}

@end
