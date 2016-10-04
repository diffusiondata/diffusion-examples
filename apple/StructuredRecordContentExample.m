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

#import "StructuredRecordContentExample.h"

@import Diffusion;

@interface StructuredRecordContentExample (PTDiffusionTopicStreamDelegate) <PTDiffusionTopicStreamDelegate>
@end

@implementation StructuredRecordContentExample {
    PTDiffusionSession* _session;
    PTDiffusionRecordContentSchema* _exchangeRatesSchema;
    NSMutableDictionary* _schemas; // Key is PTDiffusionStream, Value is PTDiffusionRecordContentSchema
    NSMutableDictionary* _describers; // Key is PTDiffusionStream, Value is RecordDescriber
}

typedef NSString* (^RecordDescriber)(PTDiffusionRecordContent * recordContent);

static NSString *const _FromKey = @"from";
static NSString *const _ToKey = @"to";

static NSString *const _CurrencyKey = @"currency";
static NSString *const _RateKey = @"rate";

static NSString *const _RootTopicPath = @"StaticMetadataPublishingClient";

static RecordDescriber _ExchangeRatesRecordDescriber = ^ NSString* (PTDiffusionRecordContent *const recordContent)
{
    PTDiffusionRecord *const from = [recordContent recordWithName:_FromKey];
    PTDiffusionField *const fromCurrency = [from fieldWithName:_CurrencyKey];

    NSMutableString *const s = [NSMutableString stringWithFormat:@"Buy 1 %@ for", fromCurrency];

    NSArray *const toRecords = [recordContent recordsWithName:_ToKey];
    NSUInteger i = 0;
    for (PTDiffusionRecord *const to in toRecords) {
        if (0 != i++) {
            [s appendString:@","];
        }
        PTDiffusionField *const toCurrency = [to fieldWithName:_CurrencyKey];
        PTDiffusionField *const toRate = [to fieldWithName:_RateKey];
        [s appendFormat:@" %@ %@", toRate, toCurrency];
    }

    return [s copy];
};

-(void)startWithURL:(NSURL*)url
 sessionConfiguration:(PTDiffusionSessionConfiguration*)sessionConfiguration {

    PTDiffusionRecordContentSchema * const exchangeRatesSchema = [[PTDiffusionRecordContentSchema alloc] initWithRecordMetadata:@[
        [[PTDiffusionRecordMetadata alloc] initWithName:_FromKey
                                          fieldMetadata:@[
            [PTDiffusionFieldMetadata stringWithName:_CurrencyKey]
        ]],
        [[PTDiffusionRecordMetadata alloc] initWithName:_ToKey
                                           multiplicity:[PTDiffusionMultiplicity multiplicityWithMinimumCount:1]
                                          fieldMetadata:@[
            [PTDiffusionFieldMetadata stringWithName:_CurrencyKey],
            [PTDiffusionFieldMetadata numberWithName:_RateKey scale:4],
        ]]
    ]];

    _schemas = [NSMutableDictionary new];
    _describers = [NSMutableDictionary new];

    NSLog(@"Connecting...");
    [PTDiffusionSession openWithURL:url
                      configuration:sessionConfiguration
                  completionHandler:^(PTDiffusionSession * const session, NSError * const error)
     {
         if (!session) {
             NSLog(@"Failed to open session: %@", error);
             return;
         }

         // At this point we now have a connected session.
         NSLog(@"Connected.");

         // Set ivar to maintain a strong reference to the session.
         _session = session;

         // Register self as the handler for topic updates.
         NSString *topicPath = [NSString stringWithFormat:@"*%@/Exchange Rates//", _RootTopicPath];
         [self registerStream:[session.topics addTopicStreamWithSelectorExpression:topicPath
                                                                          delegate:self]
                       schema:exchangeRatesSchema
                    describer:_ExchangeRatesRecordDescriber];

         // Request subscription.
         [self subscribeWithSession:session];
     }];
}

-(void)subscribeWithSession:(PTDiffusionSession * const)session {
    NSLog(@"Subscribing...");
    NSString *const expression = [NSString stringWithFormat:@"*%@//", _RootTopicPath];
    [session.topics subscribeWithTopicSelectorExpression:expression
                                       completionHandler:^(NSError * const error)
    {
        if (error) {
            NSLog(@"Subscribe request failed. Error: %@", error);
        } else {
            NSLog(@"Subscribe request succeeded.");
        }
    }];
}

-(void)registerStream:(PTDiffusionStream * const)stream
               schema:(PTDiffusionRecordContentSchema * const)schema
            describer:(const RecordDescriber)describer {
    _schemas[stream] = schema;
    _describers[stream] = describer;
}

@end

@implementation StructuredRecordContentExample (PTDiffusionTopicStreamDelegate)

-(void)diffusionStream:(PTDiffusionStream * const)stream
    didUpdateTopicPath:(NSString * const)topicPath
               content:(PTDiffusionContent * const)content
               context:(PTDiffusionUpdateContext * const)context {
    PTDiffusionRecordContentSchema *const schema = _schemas[stream];
    const RecordDescriber describer = _describers[stream];

    if (!(schema || describer)) {
        [NSException raise:NSInternalInconsistencyException format:@"A registered schema and/or describer could not be found for stream."];
    }

    NSError * error;
    PTDiffusionRecordContent *const recordContent = [content recordContentWithSchema:schema error:&error];
    if (!recordContent) {
        NSLog(@"Failed to map schema to received content for topic path \"%@\". Error: %@", topicPath, error);
        return;
    }

    NSString *const description = describer(recordContent);
    NSLog(@"%@: %@", topicPath, description);
}

-(void)     diffusionStream:(PTDiffusionStream * const)stream
    didSubscribeToTopicPath:(NSString * const)topicPath
                    details:(PTDiffusionTopicDetails * const)details {
    NSLog(@"Subscribed: \"%@\" (%@)", topicPath, details);
}

-(void)         diffusionStream:(PTDiffusionStream *const)stream
    didUnsubscribeFromTopicPath:(NSString *const)topicPath
                reason:(const PTDiffusionTopicUnsubscriptionReason)reason {
    NSLog(@"Unsubscribed: \"%@\" [Reason: %@]", topicPath, PTDiffusionTopicUnsubscriptionReasonToString(reason));
}

@end
