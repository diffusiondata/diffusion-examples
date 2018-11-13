//  Diffusion Client Library for iOS, tvOS and OS X / macOS - Examples
//
//  Copyright (C) 2016, 2018 Push Technology Ltd.
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

#import "ExclusiveJSONUpdateExample.h"

@import Diffusion;

@interface ExclusiveJSONUpdateExample (UpdateSource) <PTDiffusionTopicUpdateSource>
@end

/**
 This example uses the prefered mechanism for value updating, being an
 exclusive updater, which maintains a local cache of previously sent values so
 that deltas of change may be sent to the server on each update.
 */
@implementation ExclusiveJSONUpdateExample {
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

        [self addTopic];
    }];
}

static NSString *const _TopicPath = @"Example/JSONUpdating";

-(void)addTopic {
    // Add a JSON topic without an initial value.
    [_session.topicControl addTopicWithPath:_TopicPath
                                       type:PTDiffusionTopicType_JSON
                          completionHandler:^(PTDiffusionAddTopicResult *const result, NSError *const error)
    {
        if (error) {
            NSLog(@"Failed to add topic. Error: %@", error);
        } else {
            NSLog(@"Topic %@.", result);
            [self registerUpdateSource];
        }
    }];
}

-(void)registerUpdateSource {
    [_session.topicUpdateControl registerUpdateSource:self
                                         forTopicPath:_TopicPath
                                    completionHandler:^(PTDiffusionTopicTreeRegistration *const registration, NSError *const error)
    {
        if (error) {
            NSLog(@"Failed to register topic update source. Error: %@", error);
        } else {
            NSLog(@"Topic update source registered.");
        }
    }];
}

-(void)updateTopicWithUpdater:(PTDiffusionJSONValueUpdater *const)updater {
    dispatch_after(dispatch_time(DISPATCH_TIME_NOW, (int64_t)(1.0 * NSEC_PER_SEC)),
        dispatch_get_main_queue(), ^
    {
        // Prepare data to update topic with.
        NSDictionary *const object = [self currentDateAsComponents];
        NSError * error;
        PTDiffusionJSON *const json =
            [[PTDiffusionJSON alloc] initWithObject:object
                                              error:&error];
        if (!json) {
            NSLog(@"Failed to wrap object as JSON. Error: %@", error);
            return;
        }

        // Update the topic.
        [updater updateWithTopicPath:_TopicPath
                               value:json
                   completionHandler:^(NSError *const error)
        {
            if (error) {
                NSLog(@"Failed to update topic. Error: %@", error);
            } else {
                NSLog(@"Topic updated to \"%@\"", object);

                // Update topic after a short wait.
                [self updateTopicWithUpdater:updater];
            }
        }];
    });
}

/**
 This method returns a dictionary instance suitable for being encoded to JSON
 format. For demonstration purposes we're wrapping the components of the current
 date and time.
 */
-(NSDictionary *)currentDateAsComponents {
    NSCalendar *const calendar =
        [NSCalendar calendarWithIdentifier:NSCalendarIdentifierGregorian];
    NSDate *const date = [NSDate date];
    static const NSCalendarUnit units =
        NSCalendarUnitYear | NSCalendarUnitMonth | NSCalendarUnitDay |
        NSCalendarUnitHour | NSCalendarUnitMinute | NSCalendarUnitSecond;
    NSDateComponents *const c = [calendar components:units fromDate:date];

    return @{
        @"date": @{
            @"year": @(c.year),
            @"month": @(c.month),
            @"day": @(c.day)
        },
        @"time": @{
            @"hour": @(c.hour),
            @"minute": @(c.minute),
            @"second": @(c.second)
        }
    };
}

@end

@implementation ExclusiveJSONUpdateExample (UpdateSource)

-(void)diffusionTopicTreeRegistration:(PTDiffusionTopicTreeRegistration *const)registration
                  isActiveWithUpdater:(PTDiffusionTopicUpdater *const)updater {
    NSLog(@"Topic update source is active.");
    [self updateTopicWithUpdater:updater.jsonValueUpdater];
}

-(void)diffusionTopicTreeRegistrationIsOnStandbyForUpdates:(PTDiffusionTopicTreeRegistration *const)registration {
    NSLog(@"Topic update source is on standby.");
}

-(void)diffusionTopicTreeRegistrationDidClose:(PTDiffusionTopicTreeRegistration *const)registration {
    NSLog(@"Closed");
}

-(void)diffusionTopicTreeRegistration:(PTDiffusionTopicTreeRegistration *const)registration
                     didFailWithError:(NSError *const)error {
    NSLog(@"Topic update source failed. Error: %@", error);
}

@end
