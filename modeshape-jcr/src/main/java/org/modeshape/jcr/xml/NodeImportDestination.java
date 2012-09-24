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

package org.modeshape.jcr.xml;

import org.modeshape.jcr.ExecutionContext;
import org.modeshape.jcr.value.Path;
import java.util.TreeMap;

/**
 * Interface which is expected to be implemented by clients that perform node importing via XML, using the {@link NodeImportXmlHandler}
 * class.
 *
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public interface NodeImportDestination {

    /**
     * Retrieves the execution context of the destination, in which the import will take place and which is used for creating
     * values via the {@link org.modeshape.jcr.value.ValueFactories} and for registering namespaces via
     * the {@link org.modeshape.jcr.value.NamespaceRegistry}
     *
     * @return a non-null {@link ExecutionContext}
     */
    public ExecutionContext getExecutionContext();

    /**
     * Processes the given [nodePath, parseElement] mappings, which represent the results of the xml parsing. The given map is
     * always sorted in ascending order of the node paths.
     *
     * @param parseResults a {@link TreeMap}, never null.
     */
    public void submit( TreeMap<String, NodeImportXmlHandler.ImportElement> parseResults );
}
