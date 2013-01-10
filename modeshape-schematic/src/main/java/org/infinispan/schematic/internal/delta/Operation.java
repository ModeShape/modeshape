/*
 * JBoss, Home of Professional Open Source
 * Copyright 2009 Red Hat Inc. and/or its affiliates and other
 * contributors as indicated by the @author tags. All rights reserved.
 * See the copyright.txt in the distribution for a full listing of
 * individual contributors.
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
package org.infinispan.schematic.internal.delta;

import java.util.ArrayList;
import java.util.Collection;
import org.infinispan.schematic.document.Document;
import org.infinispan.schematic.document.Immutable;
import org.infinispan.schematic.document.Path;
import org.infinispan.schematic.internal.document.MutableDocument;

/**
 * An atomic operation for SchematicValueDelta.
 * <p/>
 * 
 * @author (various)
 */
@Immutable
public abstract class Operation {

    protected final Path parentPath;
    private final int hashCode;

    protected Operation( Path parentPath,
                         int hashCode ) {
        this.parentPath = parentPath;
        this.hashCode = hashCode;
    }

    @Override
    public final int hashCode() {
        return hashCode;
    }

    public abstract void replay( MutableDocument delegate );

    public abstract void rollback( MutableDocument delegate );

    public Path getParentPath() {
        return parentPath;
    }

    protected MutableDocument mutableParent( MutableDocument delegate ) {
        MutableDocument parent = delegate;
        MutableDocument parentOfParent = null;
        Path parentPath = getParentPath();
        for (String fieldName : parentPath) {
            parentOfParent = parent;
            parent = (MutableDocument)parentOfParent.getDocument(fieldName);
        }
        // if (parent == null && parentOfParent != null) {
        // // We need to create the document in the grandParent ...
        // parent = new BasicDocument();
        // parentOfParent.put(parentPath.getLast(), parent);
        // }
        return parent;
    }

    @Override
    public abstract Operation clone();

    protected Object cloneValue( Object value ) {
        if (value == null) return null;
        if (value instanceof Document) return ((Document)value).clone();
        if (value instanceof Collection) {
            Collection<?> original = (Collection<?>)value;
            Collection<Object> copy = new ArrayList<Object>(original.size());
            for (Object v : original) {
                copy.add(cloneValue(v));
            }
            return copy;
        }
        // everything else should be immutable ...
        return value;
    }

    protected boolean equalsIfNotNull( Object obj1,
                                       Object obj2 ) {
        if (obj1 == null) {
            return obj2 == null;
        }
        return obj1.equals(obj2);
    }

}
