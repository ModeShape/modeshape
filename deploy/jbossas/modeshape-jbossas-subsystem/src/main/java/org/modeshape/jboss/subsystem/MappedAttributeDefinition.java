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
package org.modeshape.jboss.subsystem;

import java.util.List;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.modeshape.jcr.RepositoryConfiguration;

/**
 * An {@link AttributeDefinition} that is mapped directly to a field within a {@link RepositoryConfiguration}.
 */
public interface MappedAttributeDefinition {

    /**
     * Get the path to the field within the {@link RepositoryConfiguration}.
     * 
     * @return the path; never null and never empty
     */
    List<String> getPathToField();

    /**
     * Get the path to the field that contains the mapped field within the {@link RepositoryConfiguration}.
     * 
     * @return the parent path; never null but possibly empty if the mapped field is at the top-level of the configuration
     *         document
     */
    List<String> getPathToContainerOfField();

    /**
     * Get the name of the mapped field in the {@link RepositoryConfiguration}.
     * 
     * @return the field name; never null
     */
    String getFieldName();

    /**
     * Obtain from the supplied model node value the value that can be used in the RepositoryConfiguration field.
     * 
     * @param node the model node value
     * @return the field value
     * @throws OperationFailedException if there was an error obtaining the value from the model node
     */
    Object getTypedValue( ModelNode node ) throws OperationFailedException;
}
