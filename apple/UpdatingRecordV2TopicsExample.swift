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

/**
 An example of using a control client to create and update a RecordV2 topic in
 exclusive mode.

 This uses the 'TopicControl' feature to create a topic and the
 'TopicUpdateControl' feature to send updates to it.

 To send updates to a topic, the client session requires the 'update_topic'
 permission for that branch of the topic tree.

 The example can be used with or without the use of a schema. This is simply to
 demonstrate the different mechanisms and is not necessarily demonstrating the
 most efficient way to update such a topic.
 */
public class UpdatingRecordV2TopicsExample {
    private let schema: PTDiffusionRecordV2Schema?
    private let topicSpecification: PTDiffusionTopicSpecification
    private var session: PTDiffusionSession?
    private var updateSource: UpdateSource?

    /**
     Constructor.

     @param serverUrl For example "ws://diffusion.example.com"

     @param withSchema Whether schema processing should be used or not.
     */
    init(serverUrl: URL, withSchema: Bool) {
        let topicType = PTDiffusionTopicType.recordV2
        if (withSchema) {
            // Create the record schema for the rates topic, with two decimal
            // fields which are maintained to 5 decimal places.
            let schema = PTDiffusionRecordV2SchemaBuilder()
                .addRecord(withName: "Rates")
                .addDecimal(withName: "Bid", scale: 5)
                .addDecimal(withName: "Ask", scale: 5)
                .build()
            self.schema = schema
            let schemaKey = PTDiffusionTopicSpecification.schemaPropertyKey()
            let schemaValue =
                String(data: schema.jsonData(), encoding: String.Encoding.utf8)!
            topicSpecification = PTDiffusionTopicSpecification(
                type: topicType,
                properties: [schemaKey : schemaValue])
        } else {
            self.schema = nil
            topicSpecification = PTDiffusionTopicSpecification(
                type: topicType)
        }

        let configuration = PTDiffusionSessionConfiguration(
            principal: "client",
            credentials: PTDiffusionCredentials(password: "password"))

        PTDiffusionSession.open(with: serverUrl, configuration: configuration)
        { (session, error) in
            if let connectedSession = session {
                self.session = connectedSession
                UpdateSource.register(
                    topicUpdateControl: connectedSession.topicUpdateControl,
                    completionHandler: { (updateSource: UpdateSource) in
                    self.updateSource = updateSource
                })
            } else {
                print(error!)
            }
        }
    }

    public enum ExampleError : Error {
        case notConnected
        case notActiveUpdater
    }

    /**
     Adds a new conversion rate in terms of base currency and target currency.

     @param currency The base currency (e.g. GBP).

     @param targetCurrency The target currency (e.g. USD).
     */
    public func addRateTopic(currency: String, targetCurrency: String) throws {
        if let session = self.session {
            session.topicControl.add(
                withTopicPath: Static.rateTopicName(currency, targetCurrency),
                specification: topicSpecification,
                completionHandler: { (error: Error?) in
                    if let addTopicError = error {
                        print(addTopicError)
                    }
            })
        } else {
            throw ExampleError.notConnected
        }
    }

    /**
     Set a rate.

     The rate topic in question must have been added first using the
     addRateTopic method, otherwise this will fail.

     @param currency The base currency.

     @param targetCurrency The target currency.

     @param bid The new bid rate.

     @param ask The new ask rate.

     @note The bid and ask rates are entered as strings which may be a decimal
     value which will be parsed and validated, rounding to 5 decimal places.
     */
    public func setRate(
        currency: String,
        targetCurrency: String,
        bid: String,
        ask: String) throws {
        if let valueUpdater = updateSource?.valueUpdater {
            let value: PTDiffusionRecordV2

            if let schema = self.schema {
                let model = schema.createMutableModel()
                try model.setFieldValue(bid, forKey: "Bid")
                try model.setFieldValue(ask, forKey: "Ask")
                value = try model.value()
            } else {
                value = PTDiffusionRecordV2Builder()
                    .addFields([bid, ask])
                    .build()
            }

            valueUpdater.update(
                withTopicPath: Static.rateTopicName(currency, targetCurrency),
                value: value,
                completionHandler: { (error: Error?) in
                    if let updateError = error {
                        print(updateError)
                    }
            })
        } else {
            throw ExampleError.notActiveUpdater
        }
    }

    /**
     Remove a rate (removes its topic).

     @param currency The base currency.

     @param targetCurrency The target currency.
     */
    public func removeRate(currency: String, targetCurrency: String) throws {
        if let session = self.session {
            session.topicControl.removeDiscrete(
                withTopicSelectorExpression:
                    Static.rateTopicName(currency, targetCurrency),
                completionHandler: { (error: Error?) in
                    if let removeError = error {
                        print(removeError)
                    }
            })
        } else {
            throw ExampleError.notConnected
        }
    }

    /**
     Removes a currency (removes its topic and all subordinate rate topics).

     @param currency The base currency.
     */
    public func removeCurrency(_ currency: String) throws {
        if let session = self.session {
            session.topicControl.removeDiscrete(
                withTopicSelectorExpression:
                    "?" + Static.rootTopic + "/" + currency + "//",
                completionHandler: { (error: Error?) in
                    if let removeError = error {
                        print(removeError)
                    }
            })
        } else {
            throw ExampleError.notConnected
        }
    }

    private struct Static {
        static let rootTopic = "FX"

        static func rateTopicName(_ currency: String, _ targetCurrency: String) -> String {
            return Static.rootTopic + "/" + currency + "/" + targetCurrency
        }
    }

    private class UpdateSource : PTDiffusionTopicUpdateSource {
        private var updater: PTDiffusionRecordV2ValueUpdater?

        static func register(
            topicUpdateControl: PTDiffusionTopicUpdateControlFeature,
            completionHandler: @escaping (UpdateSource) -> Void) {
            let updateSource = UpdateSource()
            topicUpdateControl.register(updateSource,
                forTopicPath: Static.rootTopic)
            { (registration: PTDiffusionTopicTreeRegistration?, error: Error?) in
                if let registrationError = error {
                    print(registrationError)
                } else {
                    print("Update source registered.")
                }
            }
        }

        var valueUpdater: PTDiffusionRecordV2ValueUpdater? {
            return self.updater
        }

        func diffusionTopicTreeRegistration(
            _ registration: PTDiffusionTopicTreeRegistration,
            isActiveWith updater: PTDiffusionTopicUpdater) {
            print("Update source is active.")
            self.updater = updater.recordValueUpdater()
        }

        func diffusionTopicTreeRegistrationIsOnStandby(
            forUpdates registration: PTDiffusionTopicTreeRegistration) {
            print("Update source is on standby.")
        }

        func diffusionTopicTreeRegistrationDidClose(
            _ registration: PTDiffusionTopicTreeRegistration) {
            print("Update source closed.")
        }

        func diffusionTopicTreeRegistration(
            _ registration: PTDiffusionTopicTreeRegistration,
            didFailWithError error: Error) {
            print("Update source registration failed: \(error)")
        }
    }
}
