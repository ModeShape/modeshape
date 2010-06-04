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
/**
 * The ModeShape specification for programmatically creating JCR {@link javax.jcr.nodetype.NodeDefinition}s.
 * To use, simply obtain the JCR {@link javax.jcr.nodetype.NodeTypeManager} from the {@link javax.jcr.Workspace#getNodeTypeManager() workspace}
 * and cast to a {@link org.modeshape.jcr.JcrNodeTypeManager}.  That object can then be used to create new
 * {@link org.modeshape.jcr.JcrNodeTypeManager#createNodeDefinitionTemplate() node definition templates},
 * {@link org.modeshape.jcr.JcrNodeTypeManager#createNodeTypeTemplate() node type templates},
 * and {@link org.modeshape.jcr.JcrNodeTypeManager#createPropertyDefinitionTemplate() property definition templates},
 * and to then {@link org.modeshape.jcr.JcrNodeTypeManager#registerNodeType(javax.jcr.nodetype.NodeTypeDefinition, boolean) register} the new node types.
 * <p>
 * This design is patterned after the similar funcationality in the JCR 2.0 Public Final Draft (PFD), and will
 * eventually be migrated to implement the specification when ModeShape supports the final JCR 2.0 final specification.
 * </p>
 */

package org.modeshape.jcr.nodetype;

