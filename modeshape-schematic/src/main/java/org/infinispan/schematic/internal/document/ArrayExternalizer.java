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
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Set;
import org.infinispan.schematic.document.Array;
import org.infinispan.schematic.document.Bson;
import org.infinispan.schematic.internal.SchematicExternalizer;
import org.infinispan.schematic.internal.marshall.Ids;
import org.infinispan.commons.util.Util;

public class ArrayExternalizer extends SchematicExternalizer<Array> {

    private static final BsonReader SHARED_READER = new BsonReader();

    /** The serialVersionUID */
    private static final long serialVersionUID = 1L;

    @Override
    public void writeObject( ObjectOutput output,
                             Array array ) throws IOException {
        // Write the type byte ...
        output.writeByte(1);

        // Write the BSON ...
        Bson.write(array, output);
    }

    @Override
    public Array readObject( ObjectInput input ) throws IOException {
        // Read the type byte ...
        int type = input.readByte();
        assert type == 1;

        // Read the BSON ...
        return SHARED_READER.readArray(input);
    }

    @Override
    public Integer getId() {
        return Ids.SCHEMATIC_VALUE_DOCUMENT;
    }

    @SuppressWarnings( "unchecked" )
    @Override
    public Set<Class<? extends Array>> getTypeClasses() {
        return Util.<Class<? extends Array>>asSet(BasicArray.class);
    }
}
