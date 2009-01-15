/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors. 
 *
 * JBoss DNA is free software. Unless otherwise indicated, all code in JBoss DNA
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * JBoss DNA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.jcr;

import java.io.InputStream;
import java.io.Reader;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;
import javax.jcr.Item;
import javax.jcr.ItemVisitor;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import net.jcip.annotations.NotThreadSafe;
import org.jboss.dna.common.util.CheckArg;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.property.Name;
import org.jboss.dna.graph.property.Path;
import org.jboss.dna.graph.property.ValueFactories;

/**
 * @author jverhaeg
 */
@NotThreadSafe
abstract class AbstractJcrProperty extends AbstractJcrItem implements Property {

    private final Node node;
    private final ExecutionContext executionContext;
    private final Name name;

    AbstractJcrProperty( Node node,
                         ExecutionContext executionContext,
                         Name name ) {
        assert node != null;
        assert executionContext != null;
        assert name != null;
        this.node = node;
        this.executionContext = executionContext;
        this.name = name;
    }

    /**
     * {@inheritDoc}
     * 
     * @throws IllegalArgumentException if <code>visitor</code> is <code>null</code>.
     * @see javax.jcr.Item#accept(javax.jcr.ItemVisitor)
     */
    public final void accept( ItemVisitor visitor ) throws RepositoryException {
        CheckArg.isNotNull(visitor, "visitor");
        visitor.visit(this);
    }

    JcrValue<?> createValue( ValueFactories valueFactories,
                             ValueInfo valueInfo,
                             Object value ) {
        return createValue(valueFactories, valueInfo.valueClass, valueInfo.propertyType, value);
    }

    private <T> JcrValue<T> createValue( ValueFactories valueFactories,
                                         Class<T> valueClass,
                                         int propertyType,
                                         Object value ) {
        return new JcrValue<T>(valueFactories, propertyType, valueClass.cast(value));
    }

    /**
     * {@inheritDoc}
     * 
     * @throws IllegalArgumentException if <code>depth</code> is negative.
     * @see javax.jcr.Item#getAncestor(int)
     */
    public final Item getAncestor( int depth ) throws RepositoryException {
        return (depth == 0 ? this : node.getAncestor(depth - 1));
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Item#getDepth()
     */
    public int getDepth() throws RepositoryException {
        return getParent().getDepth() + 1;
    }

    final ExecutionContext getExecutionContext() {
        return executionContext;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Property#getNode()
     */
    public final Node getNode() {
        return node;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Item#getName()
     */
    public final String getName() {
        return name.getString(executionContext.getNamespaceRegistry());
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Item#getParent()
     */
    public final Node getParent() {
        return node;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Item#getPath()
     */
    public final String getPath() throws RepositoryException {
        return getPath(node.getPath(), getName());
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Item#getSession()
     */
    public final Session getSession() throws RepositoryException {
        return node.getSession();
    }

    /**
     * {@inheritDoc}
     * 
     * @return false
     * @see javax.jcr.Item#isNode()
     */
    public final boolean isNode() {
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Item#isSame(javax.jcr.Item)
     */
    @Override
    public final boolean isSame( Item otherItem ) throws RepositoryException {
        if (super.isSame(otherItem) && otherItem instanceof Property) {
            Property otherProp = (Property)otherItem;
            return (getName().equals(otherProp.getName()) && getNode().isSame(otherProp.getNode()));
        }
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @throws UnsupportedOperationException always
     * @see javax.jcr.Property#setValue(javax.jcr.Value)
     */
    public final void setValue( Value value ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @throws UnsupportedOperationException always
     * @see javax.jcr.Property#setValue(javax.jcr.Value[])
     */
    public final void setValue( Value[] values ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @throws UnsupportedOperationException always
     * @see javax.jcr.Property#setValue(java.lang.String)
     */
    public final void setValue( String value ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @throws UnsupportedOperationException always
     * @see javax.jcr.Property#setValue(java.lang.String[])
     */
    public final void setValue( String[] values ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @throws UnsupportedOperationException always
     * @see javax.jcr.Property#setValue(java.io.InputStream)
     */
    public final void setValue( InputStream value ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @throws UnsupportedOperationException always
     * @see javax.jcr.Property#setValue(long)
     */
    public final void setValue( long value ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @throws UnsupportedOperationException always
     * @see javax.jcr.Property#setValue(double)
     */
    public final void setValue( double value ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @throws UnsupportedOperationException always
     * @see javax.jcr.Property#setValue(java.util.Calendar)
     */
    public final void setValue( Calendar value ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @throws UnsupportedOperationException always
     * @see javax.jcr.Property#setValue(boolean)
     */
    public final void setValue( boolean value ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @throws UnsupportedOperationException always
     * @see javax.jcr.Property#setValue(javax.jcr.Node)
     */
    public final void setValue( Node value ) {
        throw new UnsupportedOperationException();
    }

    final class ValueInfo {

        final Class<?> valueClass;
        final int propertyType;

        ValueInfo( Object value ) {
            if (value instanceof Boolean) {
                valueClass = Boolean.class;
                propertyType = PropertyType.BOOLEAN;
            } else if (value instanceof Date) {
                valueClass = Date.class;
                propertyType = PropertyType.DATE;
            } else if (value instanceof Calendar) {
                valueClass = Calendar.class;
                propertyType = PropertyType.DATE;
            } else if (value instanceof Double) {
                valueClass = Double.class;
                propertyType = PropertyType.DOUBLE;
            } else if (value instanceof Float) {
                valueClass = Float.class;
                propertyType = PropertyType.DOUBLE;
            } else if (value instanceof Integer) {
                valueClass = Integer.class;
                propertyType = PropertyType.LONG;
            } else if (value instanceof Long) {
                valueClass = Long.class;
                propertyType = PropertyType.LONG;
            } else if (value instanceof UUID) {
                valueClass = UUID.class;
                propertyType = PropertyType.REFERENCE;
            } else if (value instanceof String) {
                valueClass = String.class;
                propertyType = PropertyType.STRING;
            } else if (value instanceof Name) {
                valueClass = Name.class;
                propertyType = PropertyType.NAME;
            } else if (value instanceof Path) {
                valueClass = Path.class;
                propertyType = PropertyType.PATH;
            } else if (value instanceof InputStream) {
                valueClass = InputStream.class;
                propertyType = PropertyType.BINARY;
            } else if (value instanceof Reader) {
                valueClass = Reader.class;
                propertyType = PropertyType.BINARY;
            } else {
                valueClass = Object.class;
                propertyType = PropertyType.BINARY;
            }
        }
    }
}
