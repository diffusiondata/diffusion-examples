//  Diffusion Client Library for iOS, tvOS and OS X / macOS - Examples
//
//  Copyright (C) 2016 - 2023 DiffusionData Ltd.
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

#import "JSONSubscribeExample.h"

@import Diffusion;

@interface JSONSubscribeExample (PTDiffusionJSONValueStreamDelegate) <PTDiffusionJSONValueStreamDelegate>
@end

/**
 This example demonstrates a client consuming JSON topics.

 It is assumed that under the FX topic there is a JSON topic for each currency
 which contains a map of conversion rates to each target currency. For example,
 FX/GBP could contain {"USD":"123.45","HKD":"456.3"}.
 
 @note For a topic updater compatible with this example, see the following
 in our Java examples: ControlClientUpdatingJSONTopics
 */
@implementation JSONSubscribeExample {
    PTDiffusionSession* _session;
}

-(void)startWithURL:(NSURL *const)url {
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

        // Register self as the fallback handler for JSON value updates.
        PTDiffusionValueStream *const valueStream =
            [PTDiffusionJSON valueStreamWithDelegate:self];

        NSError *fallbackError;
        if (![session.topics addFallbackStream:valueStream error:&fallbackError]) {
            NSLog(@"Error while adding fallback stream: %@", fallbackError.description);
        }

        // Subscribe.
        NSLog(@"Subscribing...");
        [session.topics subscribeWithTopicSelectorExpression:@"?FX/"
                                           completionHandler:^(NSError * const error)
        {
            if (error) {
                NSLog(@"Subscribe request failed. Error: %@", error);
            } else {
                NSLog(@"Subscribe request succeeded.");
            }
        }];
    }];
}

-(NSString *)currencyFromTopicPath:(NSString *const)topicPath {
    // The currency from which we're converting is the last component of the
    // topic path - e.g. topic path "FX/GBP" is currency "GBP".
    return [topicPath lastPathComponent];
}

@end

@implementation JSONSubscribeExample (PTDiffusionJSONValueStreamDelegate)

-(void)     diffusionStream:(PTDiffusionStream *const)stream
    didSubscribeToTopicPath:(NSString *const)topicPath
              specification:(PTDiffusionTopicSpecification *const)specification {
    NSString *const currency = [self currencyFromTopicPath:topicPath];
    NSLog(@"Subscribed: Rates from %@", currency);
}

-(void)diffusionStream:(PTDiffusionValueStream *const)stream
    didUpdateTopicPath:(NSString *const)topicPath
         specification:(PTDiffusionTopicSpecification *const)specification
               oldJSON:(PTDiffusionJSON *const)oldJson
               newJSON:(PTDiffusionJSON *const)newJson {
    NSString *const currency = [self currencyFromTopicPath:topicPath];

    // We're assuming that the incoming JSON document is correct as expected,
    // in that the root element is a map of currencies to which we have
    // conversion rates.
    NSError * error;
    NSDictionary *const map = [newJson objectWithError:&error];
    if (!map) {
        NSLog(@"Failed to create map from received JSON. Error: %@", error);
        return;
    }

    // For the purposes of a meaningful example, only emit a log line if we
    // have a rate for GBP to USD.
    if ([currency isEqualToString:@"GBP"]) {
        const id rate = map[@"USD"];
        if (rate) {
            NSLog(@"Rate for GBP to USD: %@", rate);
        }
    }
}

-(void)         diffusionStream:(PTDiffusionStream *const)stream
    didUnsubscribeFromTopicPath:(NSString *const)topicPath
                  specification:(PTDiffusionTopicSpecification *const)specification
                         reason:(const PTDiffusionTopicUnsubscriptionReason)reason {
    NSString *const currency = [self currencyFromTopicPath:topicPath];
    NSLog(@"Unsubscribed: Rates from %@", currency);
}

-(void)diffusionDidCloseStream:(PTDiffusionStream *const)stream {
    NSLog(@"Closed");
}

-(void)diffusionStream:(PTDiffusionStream *const)stream
      didFailWithError:(NSError *const)error {
    NSLog(@"Failed: %@", error);
}

@end
