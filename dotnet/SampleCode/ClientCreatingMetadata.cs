/**
 * Copyright © 2014, 2016 Push Technology Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

using PushTechnology.ClientInterface.Client.Content.Metadata;
using PushTechnology.ClientInterface.Client.Factories;

namespace Examples {
    /// <summary>
    /// This example class has a number of methods that demonstrate the creation of metadata using the Diffusion
    /// Unified API.
    ///
    /// Metadata is normally created by a control client in order to create topics, but a standard client could create
    /// metadata to interpret content (however, it would make much more sense to get the details of the topic which
    /// would include its metadata).
    ///
    /// This example shows the creation of various types of metadata in different methods. Content metadata is used by
    /// record type topics and field metadata is used by single value topics.
    ///
    /// Note that no client session is required in order to create metadata.
    /// </summary>
    public class ClientCreatingMetadata {
        private readonly IMetadataFactory factory = Diffusion.Metadata;

        /// <summary>
        /// Example of using a decimal field builder.
        ///
        /// This is purely to demonstrate the use of a builder as in most cases one of the factory convenience methods
        /// would be much easier to use to create a new field metadata definition.
        /// </summary>
        /// <param name="name">The field name.</param>
        /// <param name="scale">The scale of the decimal value, that is, the number of digits to the right of the
        /// decimal point.</param>
        /// <param name="value">The default value.</param>
        /// <returns></returns>
        public IMDecimalString CreateDecimal( string name, int scale, double value ) {
            return factory.DecimalBuilder( name ).SetScale( scale ).SetDefaultValue( value ).Build();
        }

        /// <summary>
        /// Creates a simple name and address record definition with fixed name single multiplicity fields.
        /// </summary>
        /// <returns>The record metadata definition.</returns>
        public IMRecord CreateNameAndAddressRecord() {
            return factory.Record( "NameAndAddress", factory.String( "FirstName" ), factory.String( "Surname" ),
                factory.String( "HouseNumber" ), factory.String( "Street" ), factory.String( "Town" ),
                factory.String( "State" ), factory.String( "PostCode" ) );
        }

        /// <summary>
        /// This creates a record with two fields, a string called "Currency" and a single decimal string called "Rate"
        /// with a default value of 1.00.
        /// </summary>
        /// <returns>The record metadata definition.</returns>
        public IMRecord CreateCurrentRecord() {
            return factory.Record( "CurrencyRecord", factory.String( "Currency" ), factory.Decimal( "Rate", "1.00" ) );
        }

        /// <summary>
        /// This creates a record with two fields, a string called "Currency" and a decimal string called "Rate" with a
        /// default value of 1.00 which repeats a specified number of times.
        /// </summary>
        /// <param name="name">The record name.</param>
        /// <param name="occurs">The number of occurrences of the "Rate" field.</param>
        /// <returns>The record metadata definition.</returns>
        public IMRecord CreateMultipleRateCurrencyRecord( string name, int occurs ) {
            return factory.RecordBuilder( name )
                .Add( factory.String( "Currency" ) )
                .Add( factory.Decimal( "Rate", "1.00" ), occurs )
                .Build();
        }

        /// <summary>
        /// A simple example of creating content metadata with two records.
        /// </summary>
        /// <returns>The content metadata.</returns>
        public IMContent CreateContent() {
            return factory.Content( "Content", factory.Record( "Rec1", factory.String( "A" ), factory.String( "B" ) ),
                factory.Record( "Rec2", factory.String( "C" ) ) );
        }

        /// <summary>
        /// An example of how to use a content builder to create content metadat with a single record type that can
        /// occur zero to n times.
        /// </summary>
        /// <returns>The content metadata.</returns>
        public IMContent CreateContentRepeating() {
            return factory.ContentBuilder( "Content" )
                .Add( factory.Record( "Rec1", factory.String( "A" ), factory.String( "B" ) ), 0, -1 )
                .Build();
        }
    }
}