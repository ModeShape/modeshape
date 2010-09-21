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
package org.modeshape.graph.sequencer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import net.jcip.annotations.NotThreadSafe;
import org.modeshape.common.SystemFailureException;
import org.modeshape.common.util.StringUtil;
import org.modeshape.graph.JcrLexicon;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.PathFactory;
import org.modeshape.graph.property.Property;

/**
 * @author Randall Hauch
 * @author John Verhaeg
 */
@NotThreadSafe
public class MockSequencerOutput implements SequencerOutput, Iterable<Path> {

    private final Map<Path, Map<Name, Property>> propertiesByPath;
    private final StreamSequencerContext context;
    private final LinkedList<Path> nodePathsInCreationOrder;

    public MockSequencerOutput( StreamSequencerContext context ) {
        this(context, false);
    }

    public MockSequencerOutput( StreamSequencerContext context,
                                boolean recordOrderOfNodeCreation ) {
        this.context = context;
        this.propertiesByPath = new HashMap<Path, Map<Name, Property>>();
        this.nodePathsInCreationOrder = recordOrderOfNodeCreation ? new LinkedList<Path>() : null;
    }

    /**
     * {@inheritDoc}
     */
    public void setProperty( Path nodePath,
                             Name propertyName,
                             Object... values ) {
        Map<Name, Property> properties = propertiesByPath.get(nodePath);
        if (values == null || values.length == 0 || (values.length == 1 && values[0] == null)) {
            // remove the property ...
            if (properties != null) {
                properties.remove(propertyName);
                if (properties.isEmpty()) {
                    propertiesByPath.remove(nodePath);
                    if (nodePathsInCreationOrder != null) nodePathsInCreationOrder.remove(nodePath);
                }
            }
        } else {
            if (properties == null) {
                properties = new HashMap<Name, Property>();
                propertiesByPath.put(nodePath, properties);
                if (nodePathsInCreationOrder != null) nodePathsInCreationOrder.add(nodePath);
            }
            for (Object value : values) {
                if (value instanceof Property || value instanceof Iterator<?>) {
                    RuntimeException t = new SystemFailureException("Value of property is another property object");
                    t.printStackTrace();
                    throw t;
                }
                if (value instanceof Object[]) {
                    for (Object nestedValue : (Object[])value) {
                        if (nestedValue instanceof Property || value instanceof Iterator<?>) {
                            RuntimeException t = new SystemFailureException("Value of property is another property object");
                            t.printStackTrace();
                            throw t;
                        }
                    }
                }
            }
            Property property = context.getPropertyFactory().create(propertyName, values);
            properties.put(propertyName, property);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setProperty( String nodePath,
                             String propertyName,
                             Object... values ) {
        Path path = context.getValueFactories().getPathFactory().create(nodePath);
        Name name = context.getValueFactories().getNameFactory().create(propertyName);
        setProperty(path, name, values);
    }

    /**
     * {@inheritDoc}
     */
    public void setReference( String nodePath,
                              String propertyName,
                              String... paths ) {
        PathFactory pathFactory = context.getValueFactories().getPathFactory();
        Path path = pathFactory.create(nodePath);
        Name name = context.getValueFactories().getNameFactory().create(propertyName);
        Object[] values = null;
        if (paths != null && paths.length != 0) {
            values = new Path[paths.length];
            for (int i = 0, len = paths.length; i != len; ++i) {
                String pathValue = paths[i];
                values[i] = pathFactory.create(pathValue);
            }
        }
        setProperty(path, name, values);
    }

    public LinkedList<Path> getOrderOfCreation() {
        return nodePathsInCreationOrder;
    }

    public boolean exists( Path nodePath ) {
        return this.propertiesByPath.containsKey(nodePath);
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Iterable#iterator()
     */
    public Iterator<Path> iterator() {
        if (nodePathsInCreationOrder != null) {
            return nodePathsInCreationOrder.iterator();
        }
        LinkedList<Path> paths = new LinkedList<Path>(propertiesByPath.keySet());
        Collections.sort(paths);
        return paths.iterator();
    }

    public Map<Name, Property> getProperties( Path nodePath ) {
        Map<Name, Property> properties = this.propertiesByPath.get(nodePath);
        if (properties == null) return null;
        return Collections.unmodifiableMap(properties);
    }

    public Property getProperty( String nodePath,
                                 String propertyName ) {
        Path path = context.getValueFactories().getPathFactory().create(nodePath);
        Name name = context.getValueFactories().getNameFactory().create(propertyName);
        return getProperty(path, name);
    }

    public Property getProperty( Path nodePath,
                                 Name propertyName ) {
        Map<Name, Property> properties = this.propertiesByPath.get(nodePath);
        if (properties == null) return null;
        return properties.get(propertyName);
    }

    public Object[] getPropertyValues( String nodePath,
                                       String propertyName ) {
        Path path = context.getValueFactories().getPathFactory().create(nodePath);
        return getPropertyValues(path, propertyName);
    }

    public Object[] getPropertyValues( Path path,
                                       String propertyName ) {
        Name name = context.getValueFactories().getNameFactory().create(propertyName);
        Property prop = getProperty(path, name);
        if (prop != null) {
            return prop.getValuesAsArray();
        }
        return null;
    }

    public boolean hasProperty( String nodePath,
                                String property ) {
        return getProperty(nodePath, property) != null;
    }

    public boolean hasProperties() {
        return this.propertiesByPath.size() > 0;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Path path : this) {
            if (path == null) {
                continue;
            }
            if (!path.isRoot()) {
                sb.append(StringUtil.createString(' ', path.size() * 2));
                sb.append(path.getLastSegment().getString(context.getNamespaceRegistry()));
            } else {
                sb.append("/");
            }
            Property primaryType = getProperty(path, JcrLexicon.PRIMARY_TYPE);
            if (primaryType != null) {
                sb.append(" " + primaryType.getString(context.getNamespaceRegistry()));
            }
            Property mixinTypes = getProperty(path, JcrLexicon.MIXIN_TYPES);
            if (mixinTypes != null) {
                sb.append(" " + mixinTypes.getString(context.getNamespaceRegistry()));
            }
            Property uuid = getProperty(path, JcrLexicon.UUID);
            if (uuid != null) {
                sb.append(" " + uuid.getString(context.getNamespaceRegistry()));
            }
            sb.append("\n");
            List<Property> props = new ArrayList<Property>(getProperties(path).values());
            Collections.sort(props);
            for (Property property : props) {
                if (property.equals(primaryType)) continue;
                if (property.equals(mixinTypes)) continue;
                if (property.equals(uuid)) continue;
                sb.append(StringUtil.createString(' ', path.size() * 2)).append("  - ");
                sb.append(property.getString(context.getNamespaceRegistry()));
                sb.append("\n");
            }
        }
        return sb.toString();
    }
}
