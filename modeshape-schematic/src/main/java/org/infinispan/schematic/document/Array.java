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
package org.infinispan.schematic.document;

import java.util.List;

/**
 * Primary read-only interface for an in-memory representation of JSON/BSON arrays. Note that this interface extends
 * {@link Document}, where the field names are simply string representations of the array indices. This interface also extends the
 * standard Java {@link List} interface.
 * 
 * @author Randall Hauch <rhauch@redhat.com> (C) 2011 Red Hat Inc.
 * @since 5.1
 */
public interface Array extends Document, List<Object> {

    /**
     * Obtain a clone of this array.
     * 
     * @return the clone of this array; never null
     */
    @Override
    Array clone();

    /**
     * Get the entries in this array.
     * 
     * @return an iterable containing the array's entries; never null
     */
    Iterable<Entry> getEntries();

    /**
     * A representation of an entry within the array.
     */
    static interface Entry extends Comparable<Entry> {
        /**
         * Get the index for this entry.
         * 
         * @return the index
         */
        int getIndex();

        /**
         * Get the value for this entry.
         * 
         * @return the value
         */
        Object getValue();
    }

}
