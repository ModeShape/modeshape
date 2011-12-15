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
 * Many repositories are used (at least in part) to manage files and other artifacts, including service definitions, 
 * policy files, images, media, documents, presentations, application components, reusable libraries, configuration files, 
 * application installations, databases schemas, management scripts, and so on. Most JCR repository implementations will 
 * store those files and maybe index them for searching.
 * <p>
 * But ModeShape does more. ModeShape sequencers can automatically unlock the structured information buried within 
 * all of those files, and this useful content derived from your files is then stored back in the repository where 
 * your client applications can search, access, and analyze it using the JCR API.
 * </p>
 * <p>
 * A repository can be configured to have any number of sequencers, and each one is configured to apply to content 
 * in the repository matching specific patterns. When content in the repository changes, ModeShape automatically 
 * looks to see which (if any) sequencers might be able to run on the changed content. If any of the sequencers 
 * do match, ModeShape automatically calls them by supplying the changed content. At that point, the sequencer's 
 * job is to process the supplied input, extract meaningful information, and write that derived information back 
 * into the repository where it can be accessed, searched and used by your client applications.
 * </p>
 * <p>
 * Implementing a sequencer is pretty straightforward:
 * <ol>
 * <li>Create a class that subclasses the {@link org.modeshape.jcr.api.sequencer.Sequencer} abstract base class.</li>
 * <li>Implement the 
 * {@link org.modeshape.jcr.api.sequencer.Sequencer#execute(javax.jcr.Property, javax.jcr.Node, org.modeshape.jcr.api.sequencer.Sequencer.Context) execute(...)}
 * method by processing the supplied input property and creating a node (or subgraph) that represents the structured output of the
 * input property.</li>
 * <li>If the output structure uses custom node types, overwrite the {@link org.modeshape.jcr.api.sequencer.Sequencer#initialize(javax.jcr.NamespaceRegistry, org.modeshape.jcr.api.nodetype.NodeTypeManager) initialize(...)}
 * method to register any custom node types and namespaces. Be sure your method only registers them if they don't exist.</li>
 * <li>Configure a repository to use your new sequencer. Be sure your class (and any third-party libraries it uses)
 * are available on the classpath.</li>
 * </ol>
 * <p>
 * The output structure can take almost any form, and it is almost always specific to each kind of file being sequenced.
 * For example, ModeShape comes with an image sequencer that extracts the simple metadata from different kinds of image files 
 * (e.g., JPEG, GIF, PNG, etc.). Another example is the Compact Node Definition (CND) sequencer that processes 
 * the CND files to extract and produce a structured representation of the node type definitions, property definitions, 
 * and child node definitions contained within the file.
 * </p>
 * <p>
 * There may be multiple sequencers that all output the same type of information. For example, ModeShape's 
 * Java class file sequencer and Java source file sequencer both generate an output structure that represents the
 * Java AST. In this case, both types of files contain semantically similar information.
 * </p>
 */

package org.modeshape.jcr.api.sequencer;

