//  Diffusion Client Library for iOS, tvOS and OS X / macOS - Examples
//
//  Copyright (C) 2017 - 2022 Push Technology Ltd.
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
 This example demonstrates a client consuming RecordV2 topics.

 It has been contrived to demonstrate the various techniques for Diffusion
 record topics and is not necessarily realistic or efficient in its processing.

 It can be run using a schema or not using a schema and demonstrates how the
 processing could be done in both cases.

 This makes use of the 'Topics' feature only.

 To subscribe to a topic, the client session must have the 'select_topic' and
 'read_topic' permissions for that branch of the topic tree.

 This example receives updates to currency conversion rates via a branch of the
 topic tree where the root topic is called "FX" which under it has a topic for
 each base currency and under each of those is a topic for each target currency
 which contains the bid and ask rates. So a topic FX/GBP/USD would contain the
 rates for GBP to USD.

 This example maintains a local map of the rates and also notifies a listener of
 any rates changes.
 */
public class ClientConsumingRecordV2Topics {
    private static let rootTopic = "FX"
    private var subscriber: Subscriber?

    /**
     Constructor.
     
     @param serverUrl For example "ws://diffusion.example.com"

     @param listener An object that will be notified of rates and rate changes.
     
     @param withSchema Whether schema processing should be used or not.
     */
    init(serverUrl: URL, listener: RatesListener, withSchema: Bool) {
        let schema =
            withSchema ? ClientConsumingRecordV2Topics.createSchema() : nil

        let configuration = PTDiffusionSessionConfiguration(
            principal: "client",
            credentials: PTDiffusionCredentials(password: "password"))

        PTDiffusionSession.open(with: serverUrl, configuration: configuration)
        { (session, error) in
            if let connectedSession = session {
                self.subscriber = Subscriber(connectedSession,
                    listener: listener,
                    schema: schema)
            } else {
                print(error!)
            }
        }
    }

    private class Subscriber {
        let session: PTDiffusionSession
        let valueStreamDelegate: ValueStreamDelegate

        init(_ session: PTDiffusionSession,
             listener: RatesListener,
             schema: PTDiffusionRecordV2Schema?) {
            self.session = session

            // Use the Topics feature to add a record value stream and subscribe
            // to all topics under the root.
            valueStreamDelegate = ValueStreamDelegate(
                listener: listener,
                schema: schema)
            let valueStream = PTDiffusionRecordV2.valueStream(
                with: valueStreamDelegate)
            let topics = session.topics
            let topicSelector = "?" + rootTopic + "//"
            do {
                try topics.add(valueStream, withSelectorExpression: topicSelector, error:())
            }
            catch {
                print("Error while adding stream with selector expression")
            }
            topics.subscribe(withTopicSelectorExpression: topicSelector)
            { (error) in
                if let subscriptionError = error {
                    print(subscriptionError)
                }
            }
        }
    }

    private class ValueStreamDelegate: PTDiffusionRecordV2ValueStreamDelegate {
        let listener: RatesListener
        let schema: PTDiffusionRecordV2Schema?
        var currencies = [String: Currency]()

        init(listener: RatesListener, schema: PTDiffusionRecordV2Schema?) {
            self.listener = listener
            self.schema = schema
        }

        func diffusionStream(_ stream: PTDiffusionStream,
            didSubscribeToTopicPath topicPath: String,
            specification: PTDiffusionTopicSpecification) {
            print("Value stream subscribed to topic path: \(topicPath)")
        }

        func diffusionStream(_ stream: PTDiffusionValueStream,
            didUpdateTopicPath topicPath: String,
            specification: PTDiffusionTopicSpecification,
            oldRecord: PTDiffusionRecordV2?,
            newRecord: PTDiffusionRecordV2) {
            let topicElements = elements(topicPath)

            // It is only a rate update if topic has 2 elements below root path
            if topicElements.count == 2 {
                applyUpdate(
                    currencyCode: topicElements[0],
                    targetCurrencyCode: topicElements[1],
                    oldValue: oldRecord,
                    newValue: newRecord)
            }
        }

        func diffusionStream(_ stream: PTDiffusionStream,
            didUnsubscribeFromTopicPath topicPath: String,
            specification: PTDiffusionTopicSpecification,
            reason: PTDiffusionTopicUnsubscriptionReason) {
            let topicElements = elements(topicPath)
            if topicElements.count == 2 {
                removeRate(
                    currencyCode: topicElements[0],
                    targetCurrencyCode: topicElements[1])
            } else if topicElements.count == 1 {
                removeCurrency(code: topicElements[0])
            }
        }

        func diffusionDidClose(
            _ stream: PTDiffusionStream) {
            print("Value stream closed.")
        }

        func diffusionStream(
            _ stream: PTDiffusionStream,
            didFailWithError error: Error) {
            print("Value stream failed: \(error)")
        }

        func elements(_ topicPath: String) -> [String] {
            let prefix = rootTopic + "/"
            if let prefixRange = topicPath.range(of: prefix) {
                var subPath = topicPath
                subPath.removeSubrange(prefixRange)
                return subPath.components(separatedBy: "/")
            }
            return []
        }

        func applyUpdate(
            currencyCode: String,
            targetCurrencyCode: String,
            oldValue: PTDiffusionRecordV2?,
            newValue: PTDiffusionRecordV2) {
            let currency = currencies[currencyCode] ?? Currency()
            currencies[currencyCode] = currency

            if let schema = self.schema {
                // Update using Schema.
                applyUpdate(
                    currencyCode: currencyCode,
                    targetCurrencyCode: targetCurrencyCode,
                    oldValue: oldValue,
                    newValue: newValue,
                    currency: currency,
                    schema: schema)
                return;
            }

            // Update without using Schema.
            let fields = try! newValue.fields()
            let bid = fields[0]
            let ask = fields[1]

            currency.setRatesForTargetCurrency(
                code: targetCurrencyCode,
                bid: bid,
                ask: ask)

            if let previousValue = oldValue {
                // Compare fields individually in order to determine what has
                // changed.
                let oldFields = try! previousValue.fields()
                let oldBid = oldFields[0]
                let oldAsk = oldFields[1]
                if bid != oldBid {
                    listener.onRateChange(
                        currency: currencyCode,
                        targetCurrency: targetCurrencyCode,
                        bidOrAsk: "Bid",
                        rate: bid)
                }
                if ask != oldAsk {
                    listener.onRateChange(
                        currency: currencyCode,
                        targetCurrency: targetCurrencyCode,
                        bidOrAsk: "Ask",
                        rate: ask)
                }
            } else {
                listener.onNewRate(
                    currency: currencyCode,
                    targetCurrency: targetCurrencyCode,
                    bid: bid,
                    ask: ask)
            }
        }

        func applyUpdate(
            currencyCode: String,
            targetCurrencyCode: String,
            oldValue: PTDiffusionRecordV2?,
            newValue: PTDiffusionRecordV2,
            currency: Currency,
            schema: PTDiffusionRecordV2Schema) {
            let model = try! newValue.model(with: schema)
            let bid = try! model.fieldValue(forKey: "Bid")
            let ask = try! model.fieldValue(forKey: "Ask")

            currency.setRatesForTargetCurrency(
                code: targetCurrencyCode,
                bid: bid,
                ask: ask)

            if let previousValue = oldValue {
                // Generate a structural delta to determine what has changed.
                let delta = newValue.diff(fromOriginalRecord: previousValue)
                for change in try! delta.changes(with: schema) {
                    let fieldName = change.fieldName
                    listener.onRateChange(
                        currency: currencyCode,
                        targetCurrency: targetCurrencyCode,
                        bidOrAsk: fieldName,
                        rate: try! model.fieldValue(forKey: fieldName))
                }
            } else {
                listener.onNewRate(
                    currency: currencyCode,
                    targetCurrency: targetCurrencyCode,
                    bid: bid,
                    ask: ask)
            }
        }

        func removeCurrency(code: String) {
            if let oldCurrency = currencies.removeValue(forKey: code) {
                for targetCurrencyCode in oldCurrency.targetCurrencyCodes() {
                    listener.onRateRemoved(
                        currency: code,
                        targetCurrency: targetCurrencyCode)
                }
            }
        }

        func removeRate(currencyCode: String, targetCurrencyCode: String) {
            if let currency = currencies.removeValue(forKey: currencyCode) {
                if currency.removeTargetCurrency(code: targetCurrencyCode) {
                    listener.onRateRemoved(
                        currency: currencyCode,
                        targetCurrency: targetCurrencyCode)
                }
            }
        }
    }

    /**
     Encapsulates a base currency and all of its known rates.
     */
    private class Currency {
        var rates = [String: (bid: String, ask: String)]()

        func targetCurrencyCodes() -> [String] {
            return Array(rates.keys)
        }

        func removeTargetCurrency(code: String) -> Bool {
            return nil != rates.removeValue(forKey: code)
        }

        func setRatesForTargetCurrency(code: String, bid: String, ask: String) {
            rates[code] = (bid: bid, ask: ask)
        }
    }

    /**
     Create the record schema for the rates topic, with two decimal fields which are
     maintained to 5 decimal places
     */
    private static func createSchema() -> PTDiffusionRecordV2Schema {
        return PTDiffusionRecordV2SchemaBuilder()
            .addRecord(withName: "Rates")
            .addDecimal(withName: "Bid", scale: 5)
            .addDecimal(withName: "Ask", scale: 5)
            .build()
    }
}

/**
 A listener for rates updates.
 */
public protocol RatesListener {
    /**
     Notification of a new rate or rate update.

     @param currency The base currency.
     @param targetCurrency The target currency.
     @param bid Rate.
     @param ask Rate.
     */
    func onNewRate(currency: String, targetCurrency: String, bid: String, ask: String)

    /**
     Notification of a change to the bid or ask value for a rate.

     @param currency the base currency.
     @param targetCurrency The target currency.
     @param bidOrAsk Either "Bid" or "Ask".
     @param rate The new rate.
     */
    func onRateChange(currency: String, targetCurrency: String, bidOrAsk: String, rate: String)

    /**
     Notification of a rate being removed.

     @param currency The base currency.
     @param targetCurrency The target currency.
     */
    func onRateRemoved(currency: String, targetCurrency: String)
}
