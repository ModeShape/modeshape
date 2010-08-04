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
package org.modeshape.common.util;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.modeshape.common.text.Inflector;

/**
 * A representation of a property on a Java object.
 */
public class ObjectProperty implements Comparable<ObjectProperty>, Serializable {

    private static final long serialVersionUID = 1L;

    private static final Inflector INFLECTOR = Inflector.getInstance();

    private String name;
    private String label;
    private String description;
    private Object value;
    private Collection<?> allowedValues;
    private Class<?> type;
    private boolean readOnly;
    private String category;

    /**
     * Create a new object property that has no fields initialized.
     */
    public ObjectProperty() {
    }

    /**
     * Create a new object property with the supplied parameters set.
     * 
     * @param name the property name; may be null
     * @param value the value; may be null
     * @param label the human-readable property label; may be null
     * @param description the description for this property; may be null
     * @param readOnly true if the property is read-only, or false otherwise
     */
    public ObjectProperty( String name,
                           Object value,
                           String label,
                           String description,
                           boolean readOnly ) {
        this(name, value, label, description, null, readOnly, null);
    }

    /**
     * Create a new object property with the supplied parameters set.
     * 
     * @param name the property name; may be null
     * @param value the value; may be null
     * @param label the human-readable property label; may be null
     * @param description the description for this property; may be null
     * @param category the category for this property; may be null
     * @param readOnly true if the property is read-only, or false otherwise
     * @param type the value class; may be null
     * @param allowedValues the array of allowed values, or null or empty if the values are not constrained
     */
    public ObjectProperty( String name,
                           Object value,
                           String label,
                           String description,
                           String category,
                           boolean readOnly,
                           Class<?> type,
                           Object... allowedValues ) {
        setName(name);
        setValue(value);
        if (label != null) setLabel(label);
        if (description != null) setDescription(description);
        setCategory(category);
        setReadOnly(readOnly);
        setType(type);
        setAllowedValues(allowedValues);
    }

    /**
     * Get the property name in camel case. The getter method is simply "get" followed by the name of the property (with the first
     * character of the property converted to uppercase). The setter method is "set" (or "is" for boolean properties) followed by
     * the name of the property (with the first character of the property converted to uppercase).
     * 
     * @return the property name; may be null
     */
    public String getName() {
        return name;
    }

    /**
     * Set the property name in camel case. The getter method is simply "get" followed by the name of the property (with the first
     * character of the property converted to uppercase). The setter method is "set" (or "is" for boolean properties) followed by
     * the name of the property (with the first character of the property converted to uppercase).
     * 
     * @param name the nwe property name; may be null
     */
    public void setName( String name ) {
        this.name = name;
        if (this.label == null) setLabel(null);
    }

    /**
     * Get the human-readable label for the property. This is often just a {@link Inflector#humanize(String, String...) humanized}
     * form of the {@link #getName() property name}.
     * 
     * @return label the human-readable property label; may be null
     */
    public String getLabel() {
        return label;
    }

    /**
     * Set the human-readable label for the property. If null, this will be set to the
     * {@link Inflector#humanize(String, String...) humanized} form of the {@link #getName() property name}.
     * 
     * @param label the new label for the property; may be null
     */
    public void setLabel( String label ) {
        if (label == null && name != null) {
            label = INFLECTOR.humanize(INFLECTOR.underscore(name));
        }
        this.label = label;
    }

    /**
     * Get the description for this property.
     * 
     * @return the description; may be null
     */
    public String getDescription() {
        return description;
    }

    /**
     * Set the description for this property.
     * 
     * @param description the new description for this property; may be null
     */
    public void setDescription( String description ) {
        this.description = description;
    }

    /**
     * Get the current value for this property.
     * 
     * @return the current value; may be null
     */
    public Object getValue() {
        return value;
    }

    /**
     * Set the new value for this property.
     * 
     * @param value the new value; may be null
     */
    public void setValue( Object value ) {
        this.value = value;
        if (type == null && value != null) type = value.getClass();
    }

    /**
     * Return whether this property is read-only.
     * 
     * @return true if the property is read-only, or false otherwise
     */
    public boolean isReadOnly() {
        return readOnly;
    }

    /**
     * Set whether this property is read-only.
     * 
     * @param readOnly true if the property is read-only, or false otherwise
     */
    public void setReadOnly( boolean readOnly ) {
        this.readOnly = readOnly;
    }

    /**
     * Get the name of the category in which this property belongs.
     * 
     * @return the category name; may be null
     */
    public String getCategory() {
        return category;
    }

    /**
     * Set the name of the category in which this property belongs.
     * 
     * @param category the category name; may be null
     */
    public void setCategory( String category ) {
        this.category = category;
    }

    /**
     * Get the class to which the value must belong (excluding null values).
     * 
     * @return the value class; may be null
     */
    public Class<?> getType() {
        return type;
    }

    /**
     * Set the class to which the value must belong (excluding null values).
     * 
     * @param type the value class; may be null
     */
    public void setType( Class<?> type ) {
        this.type = type;
    }

    /**
     * Determine if this is a boolean property (the {@link #getType() type} is a {@link Boolean} or boolean).
     * 
     * @return true if this is a boolean property, or false otherwise
     */
    public boolean isBooleanType() {
        return Boolean.class.equals(type) || Boolean.TYPE.equals(type);
    }

    /**
     * Get the allowed values for this property. If this is non-null and non-empty, the {@link #getValue() value} must be one of
     * these values.
     * 
     * @return collection of allowed values, or the empty set if the values are not constrained
     */
    public Collection<?> getAllowedValues() {
        return allowedValues != null ? allowedValues : Collections.emptySet();
    }

    /**
     * Set the allowed values for this property. If this is non-null and non-empty, the {@link #setValue(Object) value} is
     * expected to be one of these values.
     * 
     * @param allowedValues the collection of allowed values, or null or empty if the values are not constrained
     */
    public void setAllowedValues( Collection<?> allowedValues ) {
        this.allowedValues = allowedValues;
    }

    /**
     * Set the allowed values for this property. If this is non-null and non-empty, the {@link #setValue(Object) value} is
     * expected to be one of these values.
     * 
     * @param allowedValues the array of allowed values, or null or empty if the values are not constrained
     */
    public void setAllowedValues( Object... allowedValues ) {
        if (allowedValues != null && allowedValues.length != 0) {
            this.allowedValues = new ArrayList<Object>(Arrays.asList(allowedValues));
        } else {
            this.allowedValues = null;
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo( ObjectProperty that ) {
        if (this == that) return 0;
        if (that == null) return 1;
        int diff = ObjectUtil.compareWithNulls(this.category, that.category);
        if (diff != 0) return diff;
        diff = ObjectUtil.compareWithNulls(this.label, that.label);
        if (diff != 0) return diff;
        diff = ObjectUtil.compareWithNulls(this.name, that.name);
        if (diff != 0) return diff;
        return 0;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return HashCode.compute(this.category, this.name, this.label);
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (obj instanceof ObjectProperty) {
            ObjectProperty that = (ObjectProperty)obj;
            if (!ObjectUtil.isEqualWithNulls(this.category, that.category)) return false;
            if (!ObjectUtil.isEqualWithNulls(this.label, that.label)) return false;
            if (!ObjectUtil.isEqualWithNulls(this.name, that.name)) return false;
            if (!ObjectUtil.isEqualWithNulls(this.value, that.value)) return false;
            if (!ObjectUtil.isEqualWithNulls(this.readOnly, that.readOnly)) return false;
            return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (name != null) sb.append(name).append(" = ");
        sb.append(value);
        sb.append(" ( ");
        sb.append(readOnly ? "readonly " : "writable ");
        if (category != null) sb.append("category=\"").append(category).append("\" ");
        if (label != null) sb.append("label=\"").append(label).append("\" ");
        if (description != null) sb.append("description=\"").append(description).append("\" ");
        sb.append(")");
        return sb.toString();
    }

    /**
     * Set on the supplied target object the property described by this instance to the {@link #getValue() value}.
     * 
     * @param target the target on which the setter is to be called; may not be null
     * @throws NoSuchMethodException if a matching method is not found.
     * @throws SecurityException if access to the information is denied.
     * @throws IllegalAccessException if the setter method could not be accessed
     * @throws InvocationTargetException if there was an error invoking the setter method on the target
     * @throws IllegalArgumentException if 'target' is null or if the {@link #getValue() value} is not legal
     */
    public void setOn( Object target )
        throws SecurityException, IllegalArgumentException, NoSuchMethodException, IllegalAccessException,
        InvocationTargetException {
        CheckArg.isNotNull(target, "target");
        Reflection reflection = new Reflection(target.getClass());
        reflection.invokeSetterMethodOnTarget(name, target, value);
    }

    /**
     * Set on the supplied target object the property described by this instance to the {@link #getValue() value}.
     * 
     * @param reflection the reflection instance to use
     * @param target the target on which the setter is to be called; may not be null
     * @param propertyName the name of the Java object property; may not be null
     * @param label the new label for the property; may be null
     * @param category the category for this property; may be null
     * @param description the description for the property; may be null if this is not known
     * @param allowedValues the of allowed values, or null or empty if the values are not constrained
     * @return the representation of the Java property; never null
     * @throws NoSuchMethodException if a matching method is not found.
     * @throws SecurityException if access to the information is denied.
     * @throws IllegalAccessException if the setter method could not be accessed
     * @throws InvocationTargetException if there was an error invoking the setter method on the target
     * @throws IllegalArgumentException if there is an illegal argument during one of the invocation of methods on the target
     */
    protected static ObjectProperty get( Reflection reflection,
                                         Object target,
                                         String propertyName,
                                         String label,
                                         String category,
                                         String description,
                                         Object... allowedValues )
        throws SecurityException, IllegalArgumentException, NoSuchMethodException, IllegalAccessException,
        InvocationTargetException {
        CheckArg.isNotNull(reflection, "reflection");
        Object value = reflection.invokeGetterMethodOnTarget(propertyName, target);
        Method[] methods = reflection.findMethods("set" + propertyName, false);
        boolean readOnly = methods.length < 1;
        Class<?> type = Object.class;
        Method[] getters = reflection.findMethods("get" + propertyName, false);
        if (getters.length == 0) {
            getters = reflection.findMethods("is" + propertyName, false);
        }
        if (getters.length > 0) {
            type = getters[0].getReturnType();
        }
        return new ObjectProperty(propertyName, value, label, description, category, readOnly, type, allowedValues);
    }

    /**
     * Get the representation of the named property (with the supplied labe, category, description, and allowed values) on the
     * target object.
     * 
     * @param target the target on which the setter is to be called; may not be null
     * @param propertyName the name of the Java object property; may not be null
     * @param label the new label for the property; may be null
     * @param category the category for this property; may be null
     * @param description the description for the property; may be null if this is not known
     * @param allowedValues the of allowed values, or null or empty if the values are not constrained
     * @return the representation of the Java property; never null
     * @throws NoSuchMethodException if a matching method is not found.
     * @throws SecurityException if access to the information is denied.
     * @throws IllegalAccessException if the setter method could not be accessed
     * @throws InvocationTargetException if there was an error invoking the setter method on the target
     * @throws IllegalArgumentException if 'target' is null, or if 'propertyName' is null or empty
     */
    public static ObjectProperty get( Object target,
                                      String propertyName,
                                      String label,
                                      String category,
                                      String description,
                                      Object... allowedValues )
        throws SecurityException, IllegalArgumentException, NoSuchMethodException, IllegalAccessException,
        InvocationTargetException {
        CheckArg.isNotNull(target, "target");
        CheckArg.isNotEmpty(propertyName, "propertyName");
        Reflection reflection = new Reflection(target.getClass());
        return get(reflection, target, propertyName, null, null, description);
    }

    /**
     * Get the representation of the named property (with the supplied description) on the target object.
     * 
     * @param target the target on which the setter is to be called; may not be null
     * @param propertyName the name of the Java object property; may not be null
     * @param description the description for the property; may be null if this is not known
     * @return the representation of the Java property; never null
     * @throws NoSuchMethodException if a matching method is not found.
     * @throws SecurityException if access to the information is denied.
     * @throws IllegalAccessException if the setter method could not be accessed
     * @throws InvocationTargetException if there was an error invoking the setter method on the target
     * @throws IllegalArgumentException if 'target' is null, or if 'propertyName' is null or empty
     */
    public static ObjectProperty get( Object target,
                                      String propertyName,
                                      String description )
        throws SecurityException, IllegalArgumentException, NoSuchMethodException, IllegalAccessException,
        InvocationTargetException {
        CheckArg.isNotNull(target, "target");
        CheckArg.isNotEmpty(propertyName, "propertyName");
        return get(target, propertyName, null, null, description);
    }

    /**
     * Get the representation of the named property on the target object.
     * 
     * @param target the target on which the setter is to be called; may not be null
     * @param propertyName the name of the Java object property; may not be null
     * @return the representation of the Java property; never null
     * @throws NoSuchMethodException if a matching method is not found.
     * @throws SecurityException if access to the information is denied.
     * @throws IllegalAccessException if the setter method could not be accessed
     * @throws InvocationTargetException if there was an error invoking the setter method on the target
     * @throws IllegalArgumentException if 'target' is null, or if 'propertyName' is null or empty
     */
    public static ObjectProperty get( Object target,
                                      String propertyName )
        throws SecurityException, IllegalArgumentException, NoSuchMethodException, IllegalAccessException,
        InvocationTargetException {
        CheckArg.isNotNull(target, "target");
        CheckArg.isNotEmpty(propertyName, "propertyName");
        return get(target, propertyName, null, null, null);
    }

    /**
     * Get representations for all of the Java properties on the supplied object.
     * 
     * @param target the target on which the setter is to be called; may not be null
     * @return the list of all properties; never null
     * @throws NoSuchMethodException if a matching method is not found.
     * @throws SecurityException if access to the information is denied.
     * @throws IllegalAccessException if the setter method could not be accessed
     * @throws InvocationTargetException if there was an error invoking the setter method on the target
     * @throws IllegalArgumentException if 'target' is null, or if 'propertyName' is null or empty
     */
    public static List<ObjectProperty> getAll( Object target )
        throws SecurityException, IllegalArgumentException, NoSuchMethodException, IllegalAccessException,
        InvocationTargetException {
        Reflection reflection = new Reflection(target.getClass());
        String[] propertyNames = reflection.findGetterPropertyNames();
        List<ObjectProperty> results = new ArrayList<ObjectProperty>(propertyNames.length);
        for (String propertyName : propertyNames) {
            ObjectProperty prop = get(target, propertyName);
            results.add(prop);
        }
        return results;
    }

    /**
     * Get representations for all of the Java properties on the supplied object.
     * 
     * @param target the target on which the setter is to be called; may not be null
     * @return the map of all properties keyed by their name; never null
     * @throws NoSuchMethodException if a matching method is not found.
     * @throws SecurityException if access to the information is denied.
     * @throws IllegalAccessException if the setter method could not be accessed
     * @throws InvocationTargetException if there was an error invoking the setter method on the target
     * @throws IllegalArgumentException if 'target' is null, or if 'propertyName' is null or empty
     */
    public static Map<String, ObjectProperty> getAllByName( Object target )
        throws SecurityException, IllegalArgumentException, NoSuchMethodException, IllegalAccessException,
        InvocationTargetException {
        Reflection reflection = new Reflection(target.getClass());
        String[] propertyNames = reflection.findGetterPropertyNames();
        Map<String, ObjectProperty> results = new HashMap<String, ObjectProperty>();
        for (String propertyName : propertyNames) {
            ObjectProperty prop = get(target, propertyName);
            results.put(propertyName, prop);
        }
        return results;
    }

}
