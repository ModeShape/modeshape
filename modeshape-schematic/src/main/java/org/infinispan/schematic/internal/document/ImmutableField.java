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

import java.util.UUID;
import java.util.regex.Pattern;
import org.infinispan.schematic.document.Binary;
import org.infinispan.schematic.document.Document;
import org.infinispan.schematic.document.Document.Field;
import org.infinispan.schematic.document.Immutable;
import org.infinispan.schematic.document.Json;

@Immutable
public class ImmutableField implements Field {

    private final String name;
    private final Object value;

    public ImmutableField( String name,
                           Object value ) {
        this.name = name;
        this.value = value;
    }

    @Override
    public int compareTo( Field that ) {
        return this == that ? 0 : this.name.compareTo(that.getName());
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Object getValue() {
        return value;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (obj instanceof Field) {
            Field that = (Field)obj;
            if (!this.getName().equals(that.getName())) return true;
            return this.getValue() != null ? this.getValue().equals(that.getValue()) : that.getValue() == null;
        }
        return false;
    }

    @Override
    public String toString() {
        return Json.write(this);
    }

    @Override
    public String getValueAsString() {
        Object value = getValue();
        return (value != null && value instanceof String) ? (String)value : null;
    }

    @Override
    public Integer getValueAsInt() {
        Object value = getValue();
        return (value != null && value instanceof Integer) ? (Integer)value : null;
    }

    @Override
    public boolean getValueAsBoolean() {
        Object value = getValue();
        return (value != null && value instanceof Boolean) ? ((Boolean)value).booleanValue() : null;
    }

    @Override
    public Binary getValueAsBinary() {
        Object value = getValue();
        return (value != null && value instanceof Binary) ? (Binary)value : null;
    }

    @Override
    public Document getValueAsDocument() {
        Object value = getValue();
        return (value != null && value instanceof Document) ? (Document)value : null;
    }

    @Override
    public Number getValueAsNumber() {
        Object value = getValue();
        return (value != null && value instanceof Number) ? (Number)value : null;
    }

    @Override
    public Pattern getValueAsPattern() {
        Object value = getValue();
        return (value != null && value instanceof Pattern) ? (Pattern)value : null;
    }

    @Override
    public Double getValueAsDouble() {
        Object value = getValue();
        return (value != null && value instanceof Double) ? (Double)value : null;
    }

    @Override
    public UUID getValueAsUuid() {
        Object value = getValue();
        if (value instanceof UUID) return (UUID)value;
        if (value instanceof String) return UUID.fromString((String)value);
        return null;
    }

}
