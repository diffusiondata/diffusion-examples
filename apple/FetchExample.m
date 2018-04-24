//  Diffusion Client Library for iOS, tvOS and OS X / macOS - Examples
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

#import "FetchExample.h"

@import Diffusion;

@interface FetchExample (PTDiffusionFetchStreamDelegate) <PTDiffusionFetchStreamDelegate>
@end

@implementation FetchExample {
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

        // Send fetch request.
        [session.topics fetchWithTopicSelectorExpression:@"*Assets//" delegate:self];
    }];
}

@end

@implementation FetchExample (PTDiffusionFetchStreamDelegate)

-(void)diffusionStream:(PTDiffusionStream * const)stream
     didFetchTopicPath:(NSString * const)topicPath
               content:(PTDiffusionContent * const)content {
    NSLog(@"Fetch Result: %@ = \"%@\"", topicPath, content);
}

-(void)diffusionDidCloseStream:(PTDiffusionStream * const)stream {
    NSLog(@"Fetch stream finished.");
}

-(void)diffusionStream:(PTDiffusionStream * const)stream didFailWithError:(NSError * const)error {
    NSLog(@"Fetch stream failed error: %@", error);
}

@end
