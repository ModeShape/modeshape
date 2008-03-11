/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.dna.common.component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.jcip.annotations.Immutable;
import org.jboss.dna.common.util.ArgCheck;
import org.jboss.dna.common.util.ClassUtil;
import org.jboss.dna.common.util.StringUtil;

/**
 * @author Randall Hauch
 */
@Immutable
public class ComponentConfig implements Comparable<ComponentConfig> {

    private final String name;
    private final String description;
    private final String componentClassname;
    private final List<String> classpath;
    private final long timestamp;

    public ComponentConfig( String name, String description, String classname, String... classpath ) {
        this(name, description, System.currentTimeMillis(), classname, classpath);
    }

    public ComponentConfig( String name, String description, long timestamp, String classname, String... classpath ) {
        ArgCheck.isNotEmpty(name, "name");
        this.name = name;
        this.description = description != null ? description.trim() : "";
        this.componentClassname = classname;
        this.classpath = buildList(classpath);
        this.timestamp = timestamp;
        // Check the classname is a valid classname ...
        if (!ClassUtil.isFullyQualifiedClassname(classname)) {
            String msg = "The classname {1} specified for {2} is not a valid Java classname";
            msg = StringUtil.createString(msg, classname, name);
            throw new IllegalArgumentException(msg);
        }
    }

    /* package */static List<String> buildList( String... classpathElements ) {
        List<String> classpath = new ArrayList<String>();
        if (classpathElements != null && classpathElements.length != 0) {
            for (String classpathElement : classpathElements) {
                if (!classpath.contains(classpathElement)) classpath.add(classpathElement);
            }
        }
        return Collections.unmodifiableList(classpath);
    }

    public String getName() {
        return this.name;
    }

    public String getDescription() {
        return this.description;
    }

    public String getComponentClassname() {
        return this.componentClassname;
    }

    public List<String> getComponentClasspath() {
        return this.classpath;
    }

    public String[] getComponentClasspathArray() {
        return this.classpath.toArray(new String[this.classpath.size()]);
    }

    /**
     * Get the system timestamp when this configuration object was created.
     * @return timestamp
     */
    public long getTimestamp() {
        return this.timestamp;
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
            if (this.isSame(that)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Determine whether this configuration represents the same configuration as that supplied.
     * @param that the other configuration to be compared with this
     * @return true if this configuration and the supplied configuration
     */
    public boolean isSame( ComponentConfig that ) {
        if (that == this) return true;
        if (that == null) return false;
        return this.getName().equalsIgnoreCase(that.getName());
    }

    public boolean hasChanged( ComponentConfig that ) {
        assert this.isSame(that);
        if (!this.getDescription().equals(that.getDescription())) return true;
        if (!this.getComponentClassname().equals(that.getComponentClassname())) return true;
        if (!this.getComponentClasspath().equals(that.getComponentClasspath())) return true;
        return false;
    }

}
