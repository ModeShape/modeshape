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
package org.modeshape.common.component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.jcip.annotations.Immutable;
import org.modeshape.common.CommonI18n;
import org.modeshape.common.annotation.Category;
import org.modeshape.common.annotation.Description;
import org.modeshape.common.annotation.Label;
import org.modeshape.common.util.CheckArg;
import org.modeshape.common.util.ClassUtil;

/**
 * An immutable configuration for a {@link Component}.
 */
@Immutable
public class ComponentConfig implements Comparable<ComponentConfig> {

    @Description( i18n = CommonI18n.class, value = "componentConfigNamePropertyDescription" )
    @Label( i18n = CommonI18n.class, value = "componentConfigNamePropertyLabel" )
    @Category( i18n = CommonI18n.class, value = "componentConfigNamePropertyCategory" )
    private final String name;

    @Description( i18n = CommonI18n.class, value = "componentConfigDescriptionPropertyDescription" )
    @Label( i18n = CommonI18n.class, value = "componentConfigDescriptionPropertyLabel" )
    @Category( i18n = CommonI18n.class, value = "componentConfigDescriptionPropertyCategory" )
    private final String description;

    @Description( i18n = CommonI18n.class, value = "componentConfigClassnamePropertyDescription" )
    @Label( i18n = CommonI18n.class, value = "componentConfigClassnamePropertyLabel" )
    @Category( i18n = CommonI18n.class, value = "componentConfigClassnamePropertyCategory" )
    private final String componentClassname;

    // @Description( i18n = CommonI18n.class, value = "componentConfigClasspathPropertyDescription" )
    // @Label( i18n = CommonI18n.class, value = "componentConfigClasspathPropertyLabel" )
    // @Category( i18n = CommonI18n.class, value = "componentConfigClasspathPropertyCategory" )
    private final List<String> classpath;

    private final Map<String, Object> properties;
    private final long timestamp;

    /**
     * Create a component configuration.
     * 
     * @param name the name of the configuration, which is considered to be a unique identifier
     * @param description the description
     * @param classname the name of the Java class used for the component
     * @param classpath the optional classpath (defined in a way compatible with a {@link ClassLoaderFactory}
     * @throws IllegalArgumentException if the name is null, empty or blank, or if the classname is null, empty or not a valid
     *         Java classname
     */
    public ComponentConfig( String name,
                            String description,
                            String classname,
                            String... classpath ) {
        this(name, description, System.currentTimeMillis(), Collections.<String, Object>emptyMap(), classname, classpath);
    }

    /**
     * Create a component configuration.
     * 
     * @param name the name of the configuration, which is considered to be a unique identifier
     * @param description the description
     * @param properties the mapping of properties to values that should be set through reflection after a component is
     *        instantiated with this configuration information
     * @param classname the name of the Java class used for the component
     * @param classpath the optional classpath (defined in a way compatible with a {@link ClassLoaderFactory}
     * @throws IllegalArgumentException if the name is null, empty or blank, or if the class name is null, empty or not a valid
     *         Java class name
     */
    public ComponentConfig( String name,
                            String description,
                            Map<String, Object> properties,
                            String classname,
                            String... classpath ) {
        this(name, description, System.currentTimeMillis(), properties, classname, classpath);
    }

    /**
     * Create a component configuration.
     * 
     * @param name the name of the configuration, which is considered to be a unique identifier
     * @param description the description
     * @param timestamp the timestamp that this component was last changed
     * @param properties the mapping of properties to values that should be set through reflection after a component is
     *        instantiated with this configuration information
     * @param classname the name of the Java class used for the component
     * @param classpath the optional classpath (defined in a way compatible with a {@link ClassLoaderFactory}
     * @throws IllegalArgumentException if the name is null, empty or blank, or if the classname is null, empty or not a valid
     *         Java classname
     */
    public ComponentConfig( String name,
                            String description,
                            long timestamp,
                            Map<String, Object> properties,
                            String classname,
                            String... classpath ) {
        CheckArg.isNotEmpty(name, "name");
        this.name = name.trim();
        this.description = description != null ? description.trim() : "";
        this.componentClassname = classname;
        this.classpath = buildList(classpath);
        this.timestamp = timestamp;
        this.properties = properties != null ? Collections.unmodifiableMap(new HashMap<String, Object>(properties)) : Collections.<String, Object>emptyMap();

        // Check the classname is a valid classname ...
        if (!ClassUtil.isFullyQualifiedClassname(classname)) {
            throw new IllegalArgumentException(CommonI18n.componentClassnameNotValid.text(classname, name));
        }
    }

    /* package */static List<String> buildList( String... classpathElements ) {
        List<String> classpath = null;
        if (classpathElements != null) {
            classpath = new ArrayList<String>();
            for (String classpathElement : classpathElements) {
                if (!classpath.contains(classpathElement)) classpath.add(classpathElement);
            }
            classpath = Collections.unmodifiableList(classpath);
        } else {
            classpath = Collections.emptyList(); // already immutable
        }
        return classpath;
    }

    /**
     * Get the name of this component.
     * 
     * @return the component name; never null, empty or blank
     */
    public String getName() {
        return this.name;
    }

    /**
     * Get the description for this component
     * 
     * @return the description
     */
    public String getDescription() {
        return this.description;
    }

    /**
     * Get the fully-qualified name of the Java class used for instances of this component
     * 
     * @return the Java class name of this component; never null or empty and always a valid Java class name
     */
    public String getComponentClassname() {
        return this.componentClassname;
    }

    /**
     * Get the classpath defined in terms of strings compatible with a {@link ClassLoaderFactory}.
     * 
     * @return the classpath; never null but possibly empty
     */
    public List<String> getComponentClasspath() {
        return this.classpath;
    }

    /**
     * Get the classpath defined as an array of strings compatible with a {@link ClassLoaderFactory}.
     * 
     * @return the classpath as an array; never null but possibly empty
     */
    public String[] getComponentClasspathArray() {
        return this.classpath.toArray(new String[this.classpath.size()]);
    }

    /**
     * Get the system timestamp when this configuration object was created.
     * 
     * @return the timestamp
     */
    public long getTimestamp() {
        return this.timestamp;
    }

    /**
     * Get the (unmodifiable) properties to be set through reflection on components of this type after instantiation
     * 
     * @return the properties to be set through reflection on components of this type after instantiation; never null
     */
    public Map<String, Object> getProperties() {
        return this.properties;
    }

    /**
     * {@inheritDoc}
     */
    public int compareTo( ComponentConfig that ) {
        if (that == this) return 0;
        int diff = this.getName().compareToIgnoreCase(that.getName());
        if (diff != 0) return diff;
        diff = (int)(this.getTimestamp() - that.getTimestamp());
        return diff;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return this.getName().hashCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (obj instanceof ComponentConfig) {
            ComponentConfig that = (ComponentConfig)obj;
            if (!this.getClass().equals(that.getClass())) return false;
            return this.getName().equalsIgnoreCase(that.getName());
        }
        return false;
    }

    /**
     * Determine whether this component has changed with respect to the supplied component. This method basically checks all
     * attributes, whereas {@link #equals(Object) equals} only checks the {@link #getClass() type} and {@link #getName()}.
     * 
     * @param component the component to be compared with this one
     * @return true if this componet and the supplied component have some changes, or false if they are exactly equivalent
     * @throws IllegalArgumentException if the supplied component reference is null or is not the same {@link #getClass() type} as
     *         this object
     */
    public boolean hasChanged( ComponentConfig component ) {
        CheckArg.isNotNull(component, "component");
        CheckArg.isInstanceOf(component, this.getClass(), "component");
        if (!this.getName().equalsIgnoreCase(component.getName())) return true;
        if (!this.getDescription().equals(component.getDescription())) return true;
        if (!this.getComponentClassname().equals(component.getComponentClassname())) return true;
        if (!this.getComponentClasspath().equals(component.getComponentClasspath())) return true;
        return false;
    }

}
