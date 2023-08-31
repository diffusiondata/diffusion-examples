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

#import "SubscribeUnsubscribeExample.h"

@import Diffusion;

@interface SubscribeUnsubscribeExample (PTDiffusionJSONValueStreamDelegate) <PTDiffusionJSONValueStreamDelegate>
@end

@implementation SubscribeUnsubscribeExample {
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
        self->_session = session;

        // Register self as the fallback handler for topic updates.
        PTDiffusionValueStream *const stream =
            [PTDiffusionJSON valueStreamWithDelegate:self];
        NSError *fallbackError;
        if (![session.topics addFallbackStream:stream error:&fallbackError]) {
            NSLog(@"Error while adding fallback stream: %@", fallbackError);
        }

        // Wait 5 seconds and then subscribe.
        [self performSelector:@selector(subscribe:) withObject:session afterDelay:5.0];
    }];
}

static NSString *const _TopicSelectorExpression = @"*Assets//";

-(void)subscribe:(const id)object {
    PTDiffusionSession *const session = object;

    NSLog(@"Subscribing...");
    [session.topics subscribeWithTopicSelectorExpression:_TopicSelectorExpression
                                       completionHandler:^(NSError * const error)
    {
        if (error) {
            NSLog(@"Subscribe request failed. Error: %@", error);
        } else {
            NSLog(@"Subscribe request succeeded.");

            // Wait 5 seconds and then unsubscribe.
            [self performSelector:@selector(unsubscribe:) withObject:session afterDelay:5.0];
        }
    }];
}

-(void)unsubscribe:(const id)object {
    PTDiffusionSession *const session = object;

    NSLog(@"Unsubscribing...");
    [session.topics unsubscribeFromTopicSelectorExpression:_TopicSelectorExpression
                                         completionHandler:^(NSError * const error)
    {
        if (error) {
            NSLog(@"Unsubscribe request failed. Error: %@", error);
        } else {
            NSLog(@"Unsubscribe request succeeded.");

            // Wait 5 seconds and then subscribe.
            [self performSelector:@selector(subscribe:) withObject:session afterDelay:5.0];
        }
    }];
}

@end

@implementation SubscribeUnsubscribeExample (PTDiffusionJSONValueStreamDelegate)

-(void)diffusionStream:(PTDiffusionValueStream *const)stream
    didUpdateTopicPath:(NSString *const)topicPath
         specification:(PTDiffusionTopicSpecification *const)specification
               oldJSON:(PTDiffusionJSON *const)oldJson
               newJSON:(PTDiffusionJSON *const)newJson {
    NSLog(@"\t%@ = \"%@\"", topicPath, newJson);
}

-(void)     diffusionStream:(PTDiffusionStream *const)stream
    didSubscribeToTopicPath:(NSString *const)topicPath
              specification:(PTDiffusionTopicSpecification *const)specification {
    NSLog(@"Subscribed: \"%@\" (%@)", topicPath, specification);
}

-(void)         diffusionStream:(PTDiffusionStream *)stream
    didUnsubscribeFromTopicPath:(NSString *)topicPath
                  specification:(PTDiffusionTopicSpecification *)specification
                         reason:(PTDiffusionTopicUnsubscriptionReason)reason {
    NSLog(@"Unsubscribed: \"%@\" [Reason: %@]", topicPath, PTDiffusionTopicUnsubscriptionReasonToString(reason));
}

-(void)diffusionDidCloseStream:(PTDiffusionStream *const)stream {
    NSLog(@"Closed");
}

-(void)diffusionStream:(PTDiffusionStream *const)stream
      didFailWithError:(NSError *const)error {
    NSLog(@"Failed: %@", error);
}

@end
