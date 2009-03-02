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

import javax.jcr.Value;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.PropertyDefinition;
import net.jcip.annotations.Immutable;
import org.jboss.dna.graph.property.Name;


/**
 * DNA implementation of the {@link PropertyDefinition} interface.  This implementation is immutable and has all fields initialized
 * through its constructor. 
 */
@Immutable
class JcrPropertyDefinition extends JcrItemDefinition implements PropertyDefinition {

    private final Value[] defaultValues;
    private final int requiredType;
    private final String[] valueConstraints;
    private final boolean multiple;

    JcrPropertyDefinition( JcrSession session,
                           NodeType declaringNodeType,
                           Name name,
                           int onParentVersion,
                           boolean autoCreated,
                           boolean mandatory,
                           boolean protectedItem,
                           Value[] defaultValues,
                           int requiredType,
                           String[] valueConstraints,
                           boolean multiple ) {
        super(session, declaringNodeType, name, onParentVersion, autoCreated, mandatory, protectedItem);
        this.defaultValues = defaultValues;
        this.requiredType = requiredType;
        this.valueConstraints = valueConstraints;
        this.multiple = multiple;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.nodetype.PropertyDefinition#getDefaultValues()
     */
    public Value[] getDefaultValues() {
        return defaultValues;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.nodetype.PropertyDefinition#getRequiredType()
     */
    public int getRequiredType() {
        return requiredType;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.nodetype.PropertyDefinition#getValueConstraints()
     */
    public String[] getValueConstraints() {
        return valueConstraints;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.nodetype.PropertyDefinition#isMultiple()
     */
    public boolean isMultiple() {
        return multiple;
    }

    /**
     * Creates a new <code>JcrPropertyDefinition</code> that is identical to the current object, but with the given
     * <code>declaringNodeType</code>. Provided to support immutable pattern for this class.
     * 
     * @param declaringNodeType the declaring node type for the new <code>JcrPropertyDefinition</code>
     * @return a new <code>JcrPropertyDefinition</code> that is identical to the current object, but with the given
     *         <code>declaringNodeType</code>.
     */
    JcrPropertyDefinition with( NodeType declaringNodeType ) {
        return new JcrPropertyDefinition(this.session, declaringNodeType, this.name, this.getOnParentVersion(),
                                         this.isAutoCreated(), this.isMandatory(), this.isProtected(), this.getDefaultValues(),
                                         this.getRequiredType(), this.getValueConstraints(), this.isMultiple());
    }
}
