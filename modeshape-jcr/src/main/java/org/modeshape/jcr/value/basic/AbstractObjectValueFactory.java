/*
 * ModeShape (http://www.modeshape.org)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors. 
 *
 * ModeShape is free software. Unless otherwise indicated, all code in ModeShape
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * ModeShape is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.modeshape.jcr.value.basic;

import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URI;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;
import javax.jcr.RepositoryException;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.jcr.api.value.DateTime;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.value.BinaryValue;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.Path;
import org.modeshape.jcr.value.Reference;
import org.modeshape.jcr.value.ValueFactory;

/**
 * Abstract {@link ValueFactory}.
 * 
 * @param <T> the property type
 */
@Immutable
public abstract class AbstractObjectValueFactory<T> implements ValueFactory<T> {

    protected AbstractObjectValueFactory() {
    }

    @Override
    public T create( Object value ) {
        if (value == null) return null;
        if (value instanceof String) return create((String)value);
        if (value instanceof Integer) return create(((Integer)value).intValue());
        if (value instanceof Long) return create(((Long)value).longValue());
        if (value instanceof Double) return create(((Double)value).doubleValue());
        if (value instanceof Float) return create(((Float)value).floatValue());
        if (value instanceof Boolean) return create(((Boolean)value).booleanValue());
        if (value instanceof BigDecimal) return create((BigDecimal)value);
        if (value instanceof DateTime) return create((DateTime)value);
        if (value instanceof Calendar) return create((Calendar)value);
        if (value instanceof Date) return create((Date)value);
        if (value instanceof Name) return create((Name)value);
        if (value instanceof Path) return create((Path)value);
        if (value instanceof Path.Segment) return create((Path.Segment)value);
        if (value instanceof Reference) return create((Reference)value);
        if (value instanceof NodeKey) return create((NodeKey)value);
        if (value instanceof UUID) return create((UUID)value);
        if (value instanceof URI) return create((URI)value);
        if (value instanceof BinaryValue) return create((BinaryValue)value);
        if (value instanceof javax.jcr.Binary) {
            javax.jcr.Binary jcrBinary = (javax.jcr.Binary)value;
            try {
                return create(jcrBinary.getStream());
            } catch (RepositoryException e) {
                throw new RuntimeException(e);
            }
        }
        if (value instanceof byte[]) return create((byte[])value);
        if (value instanceof InputStream) return create((InputStream)value);
        return create(value.toString());
    }
}
