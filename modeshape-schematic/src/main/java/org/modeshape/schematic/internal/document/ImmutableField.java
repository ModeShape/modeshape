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
package org.modeshape.schematic.internal.document;

import java.util.UUID;
import java.util.regex.Pattern;
import org.modeshape.schematic.annotation.Immutable;
import org.modeshape.schematic.document.Binary;
import org.modeshape.schematic.document.Document;
import org.modeshape.schematic.document.Json;

@Immutable
public class ImmutableField implements Document.Field {

    private final String name;
    private final Object value;

    public ImmutableField( String name,
                           Object value ) {
        this.name = name;
        this.value = value;
    }

    @Override
    public int compareTo( Document.Field that ) {
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
        if (obj instanceof Document.Field) {
            Document.Field that = (Document.Field)obj;
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
        return (value != null && value instanceof Boolean) ? ((Boolean)value).booleanValue() : false;
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
