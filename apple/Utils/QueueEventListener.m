//
//  Diffusion Client Library for iOS, tvOS and OS X / macOS - Examples
//
//  Copyright (C) 2021 - 2023 DiffusionData Ltd.
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

#import "QueueEventListener.h"

@import Diffusion;

@implementation QueueEventListener

- (void)diffusionQueueEventListenerRegistration:(nonnull PTDiffusionRegistration *)registration
                               didFailWithError:(nonnull NSError *)error {
    NSLog(@"Registration failed with error: %@", error.description);
}

- (void)diffusionQueueEventListenerRegistration:(nonnull PTDiffusionRegistration *)registration
                          didReportPolicyChange:(nonnull PTDiffusionClientQueuePolicy *)policy
                                     forSession:(nonnull PTDiffusionSessionId *)sessionId {
    NSLog(@"Detected policy change for Session [%@]: Conflation set to [%@]", sessionId, policy.isConflated ? @"true" : @"false");

}

- (void)diffusionQueueEventListenerRegistrationDidClose:(nonnull PTDiffusionRegistration *)registration {
    NSLog(@"Registration closed.");
}

@end
