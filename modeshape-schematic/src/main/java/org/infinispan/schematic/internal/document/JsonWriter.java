/*
 * ModeShape (http://www.modeshape.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.infinispan.schematic.internal.document;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.Date;
import java.util.UUID;
import java.util.regex.Pattern;
import org.infinispan.schematic.document.Binary;
import org.infinispan.schematic.document.Code;
import org.infinispan.schematic.document.CodeWithScope;
import org.infinispan.schematic.document.Document;
import org.infinispan.schematic.document.MaxKey;
import org.infinispan.schematic.document.MinKey;
import org.infinispan.schematic.document.ObjectId;
import org.infinispan.schematic.document.Symbol;
import org.infinispan.schematic.document.Timestamp;

/**
 * A component that writes modified JSON representations from the in-memory {@link Document} representation.
 * <p>
 * The modified JSON format is nearly identical to the JSON serialization used by MongoDB. All standard JSON values are written as
 * expected, but the types unique to BSON are written as follows:
 * <table border="1" cellspacing="0" cellpadding="3">
 * <tr>
 * <th>BSON Type</th>
 * <th>Class</th>
 * <th>Format</th>
 * <th>Example</th>
 * </tr>
 * <tr>
 * <td>Symbol</td>
 * <td>{@link Symbol}</td>
 * <td>"<i>value</i>"</td>
 * <td>"The quick brown fox"</td>
 * </tr>
 * <tr>
 * <td>Regular Expression</td>
 * <td>{@link Pattern}</td>
 * <td>{ "$regex" : "<i>pattern</i>", "$options" : "<i>flags</i>" }</td>
 * <td>{ "$regex" : "[CH]at\sin", "$options" : "im" }</td>
 * </tr>
 * <tr>
 * <td>Date</td>
 * <td>{@link Date}</td>
 * <td>{ "$date" : "<i>yyyy-MM-dd</i>T<i>HH:mm:ss</i>Z" }</td>
 * <td>{ "$date" : "2011-06-11T08:44:25Z" }</td>
 * </tr>
 * <tr>
 * <td>Timestamp</td>
 * <td>{@link Timestamp}</td>
 * <td>{ "$ts" : <i>timeValue</i>, "$inc" : <i>incValue</i> }</td>
 * <td>"\/TS("2011-06-11T08:44:25Z")\/"</td>
 * </tr>
 * <tr>
 * <td>ObjectId</td>
 * <td>{@link ObjectId}</td>
 * <td>{ "$oid" : "<i>12bytesOfIdInBase16</i>" }</td>
 * <td>{ "$oid" : "0000012c0000c8000900000f" }</td>
 * </tr>
 * <tr>
 * <td>Binary</td>
 * <td>{@link Binary}</td>
 * <td>{ "$type" : <i>typeAsInt</i>, "$base64" : "<i>bytesInBase64</i>" }"</td>
 * <td>{ "$type" : 0, "$base64" : "TWFuIGlzIGRpc3R" }"</td>
 * </tr>
 * <tr>
 * <td>UUID</td>
 * <td>{@link UUID}</td>
 * <td>{ "$uuid" : "<i>string-form-of-uuid</i>" }</td>
 * <td>{ "$uuid" : "09e0e949-bba4-459c-bb1d-9352e5ee8958" }</td>
 * </tr>
 * <tr>
 * <td>Code</td>
 * <td>{@link Code}</td>
 * <td>{ "$code" : "<i>code</i>" }</td>
 * <td>{ "$code" : "244-I2" }</td>
 * </tr>
 * <tr>
 * <td>CodeWithScope</td>
 * <td>{@link CodeWithScope}</td>
 * <td>{ "$code" : "<i>code</i>", "$scope" : <i>scope document</i> }</td>
 * <td>{ "$code" : "244-I2", "$scope" : { "name" : "Joe" } }</td>
 * </tr>
 * <tr>
 * <td>MinKey</td>
 * <td>{@link MinKey}</td>
 * <td>"MinKey"</td>
 * <td>"MinKey"</td>
 * </tr>
 * <tr>
 * <td>MaxKey</td>
 * <td>{@link MaxKey}</td>
 * <td>"MaxKey"</td>
 * <td>"MaxKey"</td>
 * </tr>
 * <tr>
 * <td>Null value</td>
 * <td></td>
 * <td>null</td>
 * <td>null</td>
 * </tr>
 * </table>
 * </p>
 * 
 * @author Randall Hauch <rhauch@redhat.com> (C) 2011 Red Hat Inc.
 * @since 5.1
 */
public interface JsonWriter {

    /**
     * Write to the supplied stream the modified JSON representation of the supplied in-memory {@link Document}.
     * 
     * @param object the BSON object or BSON value; may not be null
     * @param stream the output stream; may not be null
     * @throws IOException if there was a problem reading from the stream
     */
    void write( Object object,
                OutputStream stream ) throws IOException;

    /**
     * Write to the supplied writer the modified JSON representation of the supplied in-memory {@link Document}.
     * 
     * @param object the BSON object or BSON value; may not be null
     * @param writer the writer; may not be null
     * @throws IOException if there was a problem reading from the stream
     */
    void write( Object object,
                Writer writer ) throws IOException;

    /**
     * Write to the supplied string builder the modified JSON representation of the supplied in-memory {@link Document} .
     * 
     * @param object the BSON object or BSON value; may not be null
     * @param builder the string builder; may not be null
     */
    void write( Object object,
                StringBuilder builder );

    /**
     * Write and return the modified JSON representation of the supplied in-memory {@link Document}.
     * 
     * @param object the BSON object or BSON value; may not be null
     * @return the JSON string representation; never null
     */
    String write( Object object );
}
