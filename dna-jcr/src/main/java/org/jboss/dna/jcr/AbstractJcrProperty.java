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
import java.util.Calendar;
import javax.jcr.Item;
import javax.jcr.ItemVisitor;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.nodetype.PropertyDefinition;
import net.jcip.annotations.NotThreadSafe;
import org.jboss.dna.common.util.CheckArg;
import org.jboss.dna.graph.ExecutionContext;

/**
 * @author jverhaeg
 */
@NotThreadSafe
abstract class AbstractJcrProperty extends AbstractJcrItem implements Property {

    private final AbstractJcrNode node;
    private final org.jboss.dna.graph.property.Property dnaProperty;
    private final PropertyDefinition jcrPropertyDefinition;
    private final int propertyType;

    AbstractJcrProperty( AbstractJcrNode node,
                         PropertyDefinition definition,
                         int propertyType,
                         org.jboss.dna.graph.property.Property dnaProperty ) {
        assert node != null;
        assert dnaProperty != null;
        assert definition != null;
        this.node = node;
        this.dnaProperty = dnaProperty;
        this.jcrPropertyDefinition = definition;
        this.propertyType = propertyType;
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

    JcrValue createValue( Object value ) {
        return new JcrValue(getExecutionContext().getValueFactories(), getType(), value);
    }

    final ExecutionContext getExecutionContext() {
        return node.session().getExecutionContext();
    }

    final org.jboss.dna.graph.property.Property getDnaProperty() {
        return dnaProperty;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Property#getType()
     */
    public final int getType() {
        return propertyType;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Property#getDefinition()
     */
    public final PropertyDefinition getDefinition() {
        return jcrPropertyDefinition;
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method returns the string form of the {@link org.jboss.dna.graph.property.Property#getName()}, computed dynamically
     * each time this method is called to ensure that the property namespace prefix is used.
     * </p>
     * 
     * @see javax.jcr.Item#getName()
     */
    public final String getName() {
        return dnaProperty.getName().getString(node.namespaces());
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
    public final Session getSession() {
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
        if (otherItem instanceof Property) {
            Property otherProperty = (Property)otherItem;
            // The nodes that own the properties must be the same ...
            if (!getParent().isSame(otherProperty.getParent())) return false;
            // The properties must have the same name ...
            return getName().equals(otherProperty.getName());
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
}
