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
package org.infinispan.schematic.internal;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Set;
import org.infinispan.atomic.Delta;
import org.infinispan.atomic.DeltaAware;
import org.infinispan.marshall.SerializeWith;
import org.infinispan.schematic.SchematicEntry;
import org.infinispan.schematic.document.Document;
import org.infinispan.schematic.internal.delta.Operation;
import org.infinispan.schematic.internal.document.MutableDocument;
import org.infinispan.schematic.internal.marshall.Ids;
import org.infinispan.util.Util;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

/**
 * An implementation of the {@link Delta} for a {@link SchematicEntry}.
 * 
 * @author Randall Hauch <rhauch@redhat.com> (C) 2011 Red Hat Inc.
 * @since 5.1
 * @see org.infinispan.atomic.AtomicHashMapDelta
 */
@SerializeWith( SchematicEntryWholeDelta.Externalizer.class )
public class SchematicEntryWholeDelta implements SchematicDelta {
    private static final Log LOG = LogFactory.getLog(SchematicEntryWholeDelta.class);
    private static final boolean TRACE = LOG.isTraceEnabled();

    private final Document document;

    protected SchematicEntryWholeDelta( Document document ) {
        this.document = document;
    }

    @Override
    public DeltaAware merge( DeltaAware d ) {
        SchematicEntryLiteral other = null;
        try {
            if (d != null && (d instanceof SchematicEntryLiteral)) {
                other = (SchematicEntryLiteral)d;
                other.setDocument(document);
                LOG.trace("Merging whole doc delta into existing literal, resulting in " + other);
            } else {
                other = new SchematicEntryLiteral((MutableDocument)document);
                LOG.trace("Merging whole doc delta into new " + other);
            }
        } catch (RuntimeException e) {
            LOG.debug("Exception while merging delta " + this + " onto " + d, e);
            throw e;
        } finally {
            if (other != null) other.commit();
        }
        return other;
    }

    @Override
    public boolean isRecordingOperations() {
        return false;
    }

    @Override
    public void addOperation( Operation o ) {
        // do nothing
    }

    @Override
    public String toString() {
        return "SchematicEntryWholeDelta" + document;
    }

    @Override
    public int hashCode() {
        return document.hashCode();
    }

    @Override
    public boolean equals( Object obj ) {
        if (obj instanceof SchematicEntryWholeDelta) {
            SchematicEntryWholeDelta other = (SchematicEntryWholeDelta)obj;
            return document.equals(other.document);
        }
        return false;
    }

    public static class Externalizer extends SchematicExternalizer<SchematicEntryWholeDelta> {
        private static final long serialVersionUID = 1L;

        @SuppressWarnings( "synthetic-access" )
        @Override
        public void writeObject( ObjectOutput output,
                                 SchematicEntryWholeDelta delta ) throws IOException {
            if (TRACE) LOG.tracef("Serializing delta as document %s", delta.document);
            output.writeObject(delta.document);
        }

        @SuppressWarnings( "synthetic-access" )
        @Override
        public SchematicEntryWholeDelta readObject( ObjectInput input ) throws IOException, ClassNotFoundException {
            Document document = (Document)input.readObject();
            SchematicEntryWholeDelta delta = new SchematicEntryWholeDelta(document);
            if (TRACE) LOG.tracef("Deserialized delta as document %s", delta.document);
            return delta;
        }

        @Override
        public Integer getId() {
            return Ids.SCHEMATIC_VALUE_DELTA_DOCUMENT;
        }

        @Override
        @SuppressWarnings( "unchecked" )
        public Set<Class<? extends SchematicEntryWholeDelta>> getTypeClasses() {
            return Util.<Class<? extends SchematicEntryWholeDelta>>asSet(SchematicEntryWholeDelta.class);
        }
    }
}
