//  Diffusion Client Library for iOS, tvOS and OS X / macOS - Examples
//
//  Copyright (C) 2017 Push Technology Ltd.
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

import Foundation
import Diffusion

class MessagingToPushNotificationBridgeExample {
    static let servicePath = "push/notifications"
    var session: PTDiffusionSession?

    func startWithURL(url: NSURL) throws {
        print("Connecting...")

        PTDiffusionSession.open(with: url as URL) { (session, error) -> Void in
            if session == nil {
                print("Failed to open session: \(error!)")
                return
            }

            // At this point we now have a connected session.
            print("Connected")

            // Set ivar to maintain a strong reference to the session.
            self.session = session

            // An example APNs device token

            let tokenBytes:[UInt8] = [0x5a, 0x88, 0x3a, 0x57, 0xe2, 0x89, 0x77, 0x84,
                              0x1d, 0xc8, 0x1a, 0x0a, 0xa1, 0x4e, 0x2f, 0xdf,
                              0x64, 0xc6, 0x5a, 0x8f, 0x7b, 0xb1, 0x9a, 0xa1,
                              0x6e, 0xaf, 0xc3, 0x16, 0x13, 0x18, 0x1c, 0x97]
            let deviceToken = NSData(bytes: tokenBytes, length: 32)

            self.doPnSubscribe(topicPath: "some/topic/path", deviceToken: deviceToken)
        }
    }

    /**
     * Compose a URI understood by the Push Notification Bridge from an APNs device token.
     * @param deviceID APNS device token.
     * @return string in format expected by the push notification bridge.
     */
    func formatAsURI(token:NSData) -> String {
        return String(format:"apns://", token.base64EncodedString())
    }

    func doPnSubscribe(topicPath: String, deviceToken: NSData) {
        // Compose the JSON request from literals
        let requestDict = [
            "pnsub" : [
                "destination": formatAsURI(token: deviceToken),
                "topic": topicPath
            ]
        ]

        // Build a JSON request from that
        let json = try! PTDiffusionJSON(object: requestDict)

        session?.messaging.send(
            json.request,
            toPath: MessagingToPushNotificationBridgeExample.servicePath,
            jsonCompletionHandler: {
            (json, error) -> Void in

            if (nil == json) {
                print("Send to \"\(MessagingToPushNotificationBridgeExample.servicePath)\" failed: \(error!)")
            } else {
                print("Response: \(json!)")
            }
        })
    }
}
