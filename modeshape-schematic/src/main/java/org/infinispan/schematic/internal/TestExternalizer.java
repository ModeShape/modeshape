/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.infinispan.schematic.internal;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Set;
import org.infinispan.marshall.AdvancedExternalizer;
import org.infinispan.schematic.internal.document.MutableDocument;
import org.infinispan.schematic.internal.marshall.Ids;
import org.infinispan.util.Util;

/**
 *
 * @author kulikov
 */
public class TestExternalizer implements AdvancedExternalizer<SchematicEntryLiteral> {

    private static final long serialVersionUID = 1L;

    @Override
    public void writeObject(ObjectOutput output,
            SchematicEntryLiteral literal) throws IOException {
        output.writeObject(literal.data());
    }

    @Override
    public SchematicEntryLiteral readObject(ObjectInput input) throws IOException, ClassNotFoundException {
        MutableDocument doc = (MutableDocument) input.readObject();
        return new SchematicEntryLiteral(doc);
    }

    @Override
    public Integer getId() {
        return Ids.SCHEMATIC_VALUE_LITERAL;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Set<Class<? extends SchematicEntryLiteral>> getTypeClasses() {
        return Util.<Class<? extends SchematicEntryLiteral>>asSet(SchematicEntryLiteral.class);
    }
}
