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
package org.modeshape.sequencer.jpdl3;

/**
 * @author Serge Pagop
 */
public class JPDL3AssignmentMetadata {

    /**
     * The full qualified class name.
     */
    private String fqClassName = "";

    /**
     * The expression.
     */
    private String expression = "";

    /**
     * The config type.
     */
    private String configType = "";

    /**
     * Get the full qualified name of the class delegation.
     * 
     * @return the fqClassName.
     */
    public String getFqClassName() {
        return this.fqClassName;
    }

    /**
     * Set the full qualified name of the class delegation.
     * 
     * @param fqClassName Sets fqClassName to the specified value.
     */
    public void setFqClassName( String fqClassName ) {
        this.fqClassName = fqClassName;
    }

    /**
     * Get the assignment expression for the jpdl identity component.
     * 
     * @return the expression.
     */
    public String getExpression() {
        return this.expression;
    }

    /**
     * Set the expression.
     * 
     * @param expression Sets expression to the specified value.
     */
    public void setExpression( String expression ) {
        this.expression = expression;
    }

    /**
     * Get the configType.
     * 
     * @return configType
     */
    public String getConfigType() {
        return this.configType;
    }

    /**
     * @param configType Sets configType to the specified value.
     */
    public void setConfigType( String configType ) {
        this.configType = configType;
    }

}
