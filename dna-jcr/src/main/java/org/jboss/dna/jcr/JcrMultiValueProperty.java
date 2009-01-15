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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.nodetype.PropertyDefinition;
import net.jcip.annotations.NotThreadSafe;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.property.Name;
import org.jboss.dna.graph.property.ValueFactories;

/**
 * @author jverhaeg
 */
@NotThreadSafe
final class JcrMultiValueProperty extends AbstractJcrProperty {

    private List<JcrValue<?>> jcrValues = new ArrayList<JcrValue<?>>();

    JcrMultiValueProperty( Node node,
                           ExecutionContext executionContext,
                           Name name,
                           Iterable<?> values ) {
        super(node, executionContext, name);
        assert values != null;
        ValueFactories valueFactories = executionContext.getValueFactories();
        ValueInfo valueInfo = null;
        for (Object value : values) {
            if (valueInfo == null) {
                valueInfo = new ValueInfo(value);
            }
            jcrValues.add(createValue(valueFactories, valueInfo, value));
        }
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
     * @see javax.jcr.Property#getDefinition()
     */
    public PropertyDefinition getDefinition() {
        return new AbstractJcrPropertyDefinition() {

            public boolean isMultiple() {
                return true;
            }
        };
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
        long[] lengths = new long[jcrValues.size()];
        Iterator<JcrValue<?>> iter = jcrValues.iterator();
        for (int ndx = 0; iter.hasNext(); ndx++) {
            lengths[ndx] = iter.next().getLength();
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
     * @see javax.jcr.Property#getType()
     */
    public int getType() {
        return jcrValues.get(0).getType();
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
     * 
     * @see javax.jcr.Property#getValues()
     */
    public Value[] getValues() {
        return jcrValues.toArray(new Value[jcrValues.size()]);
    }
}
