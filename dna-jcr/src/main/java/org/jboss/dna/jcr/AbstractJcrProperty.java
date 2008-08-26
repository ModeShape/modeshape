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
import org.jboss.dna.spi.ExecutionContext;
import org.jboss.dna.spi.graph.Name;

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
     * @see javax.jcr.Item#accept(javax.jcr.ItemVisitor)
     */
    public void accept( ItemVisitor visitor ) throws RepositoryException {
        visitor.visit(this);
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
     * @see javax.jcr.Property#getDefinition()
     */
    public PropertyDefinition getDefinition() {
        throw new UnsupportedOperationException();
    }

    final ExecutionContext getExecutionContext() {
        return executionContext;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Property#getNode()
     */
    final public Node getNode() {
        return node;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Item#getName()
     */
    public final String getName() {
        return name.getString();
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
    public String getPath() throws RepositoryException {
        return node.getPath() + '/' + getName();
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
    public boolean isSame( Item otherItem ) throws RepositoryException {
        if (otherItem instanceof Property) {
            Property otherProp = (Property)otherItem;
            return (getName().equals(otherProp.getName()) && getNode().isSame(otherProp.getNode()));
        }
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Property#setValue(javax.jcr.Value)
     */
    public void setValue( Value value ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Property#setValue(javax.jcr.Value[])
     */
    public void setValue( Value[] values ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Property#setValue(java.lang.String)
     */
    public void setValue( String value ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Property#setValue(java.lang.String[])
     */
    public void setValue( String[] values ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Property#setValue(java.io.InputStream)
     */
    public void setValue( InputStream value ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Property#setValue(long)
     */
    public void setValue( long value ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Property#setValue(double)
     */
    public void setValue( double value ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Property#setValue(java.util.Calendar)
     */
    public void setValue( Calendar value ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Property#setValue(boolean)
     */
    public void setValue( boolean value ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Property#setValue(javax.jcr.Node)
     */
    public void setValue( Node value ) {
        throw new UnsupportedOperationException();
    }
}
