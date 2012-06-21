/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.infinispan.schematic.internal.document;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Set;
import org.infinispan.schematic.document.Bson;
import org.infinispan.schematic.document.Document;
import org.infinispan.schematic.internal.SchematicExternalizer;
import org.infinispan.schematic.internal.marshall.Ids;
import org.infinispan.util.Util;

public class DocumentExternalizer extends SchematicExternalizer<Document> {
    /** The serialVersionUID */
    private static final long serialVersionUID = 1L;

    @Override
    public void writeObject( ObjectOutput output,
                             Document doc ) throws IOException {
        // Write the type byte ...
        output.writeByte(1);

        // Write the BSON ...
        Bson.write(doc, output);
    }

    @Override
    public Document readObject( ObjectInput input ) throws IOException {
        // Read the type byte ...
        int type = input.readByte();
        assert type == 1;

        // Read the BSON ...
        return Bson.read(input);
    }

    @Override
    public Integer getId() {
        return Ids.SCHEMATIC_VALUE_DOCUMENT;
    }

    @SuppressWarnings( "unchecked" )
    @Override
    public Set<Class<? extends Document>> getTypeClasses() {
        return Util.<Class<? extends Document>>asSet(BasicDocument.class, BasicArray.class);
    }
}
