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
package org.jboss.dna.sequencer.java.metadata;

import java.util.ArrayList;
import java.util.List;

/**
 * Represent the {@link MethodMetadata}
 */
public abstract class MethodMetadata {

    private String name;

    private FieldMetadata returnType;

    public abstract boolean isContructor();

    private List<ModifierMetadata> modifiers = new ArrayList<ModifierMetadata>();

    private List<FieldMetadata> parameters = new ArrayList<FieldMetadata>();

    /**
     * @return name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name Sets name to the specified value.
     */
    public void setName( String name ) {
        this.name = name;
    }

    /**
     * @return modifiers
     */
    public List<ModifierMetadata> getModifiers() {
        return modifiers;
    }

    /**
     * @param modifiers Sets modifiers to the specified value.
     */
    public void setModifiers( List<ModifierMetadata> modifiers ) {
        this.modifiers = modifiers;
    }

    /**
     * @return parameters
     */
    public List<FieldMetadata> getParameters() {
        return parameters;
    }

    /**
     * @param parameters Sets parameters to the specified value.
     */
    public void setParameters( List<FieldMetadata> parameters ) {
        this.parameters = parameters;
    }

    /**
     * @return returnType
     */
    public FieldMetadata getReturnType() {
        return returnType;
    }

    /**
     * @param returnType Sets returnType to the specified value.
     */
    public void setReturnType( FieldMetadata returnType ) {
        this.returnType = returnType;
    }
}
