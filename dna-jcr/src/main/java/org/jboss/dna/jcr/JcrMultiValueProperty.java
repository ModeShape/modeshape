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
import java.util.Iterator;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.nodetype.PropertyDefinition;
import net.jcip.annotations.NotThreadSafe;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.property.Property;

/**
 * @author jverhaeg
 */
@NotThreadSafe
final class JcrMultiValueProperty extends AbstractJcrProperty {

    JcrMultiValueProperty( Node node,
                           ExecutionContext executionContext,
                           PropertyDefinition definition,
                           Property dnaProperty ) {
        super(node, executionContext, definition, dnaProperty);
    }

    /**
     * {@inheritDoc}
     * 
     * @throws ValueFormatException always
     * @see javax.jcr.Property#getBoolean()
     */
    public boolean getBoolean() throws ValueFormatException {
        throw new ValueFormatException();
    }

    /**
     * {@inheritDoc}
     * 
     * @throws ValueFormatException always
     * @see javax.jcr.Property#getDate()
     */
    public Calendar getDate() throws ValueFormatException {
        throw new ValueFormatException();
    }

    /**
     * {@inheritDoc}
     * 
     * @throws ValueFormatException always
     * @see javax.jcr.Property#getDouble()
     */
    public double getDouble() throws ValueFormatException {
        throw new ValueFormatException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Property#getNode()
     */
    public Node getNode() throws ValueFormatException, RepositoryException {
        throw new ValueFormatException();
    }

    /**
     * {@inheritDoc}
     * 
     * @throws ValueFormatException always
     * @see javax.jcr.Property#getLength()
     */
    public long getLength() throws ValueFormatException {
        throw new ValueFormatException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Property#getLengths()
     */
    public long[] getLengths() throws RepositoryException {
        Property dnaProperty = getDnaProperty();
        long[] lengths = new long[dnaProperty.size()];
        Iterator<?> iter = dnaProperty.iterator();
        for (int ndx = 0; iter.hasNext(); ndx++) {
            lengths[ndx] = createValue(iter.next()).getLength();
        }
        return lengths;
    }

    /**
     * {@inheritDoc}
     * 
     * @throws ValueFormatException always
     * @see javax.jcr.Property#getLong()
     */
    public long getLong() throws ValueFormatException {
        throw new ValueFormatException();
    }

    /**
     * {@inheritDoc}
     * 
     * @throws ValueFormatException always
     * @see javax.jcr.Property#getStream()
     */
    public InputStream getStream() throws ValueFormatException {
        throw new ValueFormatException();
    }

    /**
     * {@inheritDoc}
     * 
     * @throws ValueFormatException always
     * @see javax.jcr.Property#getString()
     */
    public String getString() throws ValueFormatException {
        throw new ValueFormatException();
    }

    /**
     * {@inheritDoc}
     * 
     * @throws ValueFormatException always
     * @see javax.jcr.Property#getValue()
     */
    public Value getValue() throws ValueFormatException {
        throw new ValueFormatException();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Per the JCR specification, these values need to be created each time this method is called, since the Value cannot be used
     * after {@link Value#getStream()} is called/processed. The spec says that the client simply needs to obtain a new Value (or
     * {@link #getValues()} for {@link JcrMultiValueProperty multi-valued properites}).
     * </p>
     * 
     * @see javax.jcr.Property#getValues()
     */
    public Value[] getValues() {
        Property dnaProperty = getDnaProperty();
        Value[] values = new JcrValue[dnaProperty.size()];
        Iterator<?> iter = dnaProperty.iterator();
        for (int ndx = 0; iter.hasNext(); ndx++) {
            values[ndx] = createValue(iter.next());
        }
        return values;
    }
}
