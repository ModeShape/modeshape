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
package org.jboss.dna.spi.graph.commands.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.jcip.annotations.NotThreadSafe;
import org.jboss.dna.spi.graph.Name;
import org.jboss.dna.spi.graph.Path.Segment;
import org.jboss.dna.spi.graph.commands.GraphCommand;
import org.jboss.dna.spi.graph.impl.BasicPathSegment;

/**
 * @author Randall Hauch
 */
@NotThreadSafe
public abstract class BasicGraphCommand implements GraphCommand {

    private boolean cancelled = false;
    private Throwable error;

    /**
     * 
     */
    public BasicGraphCommand() {
    }

    /**
     * {@inheritDoc}
     */
    public boolean isCancelled() {
        return cancelled;
    }

    /**
     * @param cancelled Sets cancelled to the specified value.
     */
    public void setCancelled( boolean cancelled ) {
        this.cancelled = cancelled;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.spi.graph.commands.GraphCommand#setError(java.lang.Throwable)
     */
    public void setError( Throwable t ) {
        error = t;
    }

    /**
     * @return error
     */
    public Throwable getError() {
        return error;
    }

    public boolean hasError() {
        return error != null;
    }

    public boolean hasNoError() {
        return error == null;
    }

    // ----------------------------------------------------------------------------------------------------------------
    // Utility methods used by concrete command implementations
    // ----------------------------------------------------------------------------------------------------------------

    protected List<Segment> createChildrenList( Name nameOfChild ) {
        if (nameOfChild == null) {
            return Collections.emptyList();
        }
        return Collections.singletonList((Segment)new BasicPathSegment(nameOfChild));
    }

    protected List<Segment> createChildrenList( Iterator<Segment> namesOfChildren ) {
        if (namesOfChildren == null) {
            return Collections.emptyList();
        }
        List<Segment> children = new ArrayList<Segment>();
        while (namesOfChildren.hasNext()) {
            children.add(namesOfChildren.next());
        }
        return children;
    }

    protected List<Segment> createChildrenList( Iterable<Segment> namesOfChildren ) {
        if (namesOfChildren == null) {
            return Collections.emptyList();
        }
        List<Segment> children = new ArrayList<Segment>();
        for (Segment childSegment : namesOfChildren) {
            children.add(childSegment);
        }
        return children;
    }

    protected static List<Segment> createChildrenList( Segment... namesOfChildren ) {
        if (namesOfChildren == null || namesOfChildren.length == 0) {
            return Collections.emptyList();
        }
        List<Segment> children = new ArrayList<Segment>();
        for (Segment childSegment : namesOfChildren) {
            children.add(childSegment);
        }
        return children;
    }

    protected static void setProperty( Map<Name, List<Object>> propertyValues,
                                       Name propertyName,
                                       Object... values ) {
        if (values == null || values.length == 0) {
            propertyValues.remove(propertyName);
        } else {
            List<Object> valuesList = null;
            if (values.length == 1) {
                Object value = values[0];
                if (value instanceof Collection<?>) {
                    setProperty(propertyValues, propertyName, ((Collection<?>)value).iterator());
                    return;
                } else if (value instanceof Iterable<?>) {
                    setProperty(propertyValues, propertyName, (Iterable<?>)value);
                    return;
                } else if (value instanceof Iterator<?>) {
                    setProperty(propertyValues, propertyName, (Iterator<?>)value);
                    return;
                }
                // Otherwise, single object is just a normal value ...
                valuesList = Collections.singletonList(value);
            } else {
                assert values.length > 1;
                valuesList = new ArrayList<Object>(values.length);
                for (Object arrayValue : values) {
                    valuesList.add(arrayValue);
                }
            }
            propertyValues.put(propertyName, valuesList);
        }
    }

    protected static void setProperty( Map<Name, List<Object>> propertyValues,
                                       Name propertyName,
                                       Iterable<?> values ) {
        if (values == null) {
            propertyValues.remove(propertyName);
        } else {
            List<Object> valuesList = new ArrayList<Object>();
            for (Object value : values) {
                valuesList.add(value);
            }
            propertyValues.put(propertyName, valuesList);
        }
    }

    protected static void setProperty( Map<Name, List<Object>> propertyValues,
                                       Name propertyName,
                                       Iterator<?> values ) {
        if (values == null) {
            propertyValues.remove(propertyName);
        } else {
            List<Object> valuesList = new ArrayList<Object>();
            while (values.hasNext()) {
                Object value = values.next();
                valuesList.add(value);
            }
            propertyValues.put(propertyName, valuesList);
        }
    }

}
