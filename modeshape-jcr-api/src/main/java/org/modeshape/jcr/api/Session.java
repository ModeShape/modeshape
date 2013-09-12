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
package org.modeshape.jcr.api;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;

/**
 * A specialization of the standard JCR {@link javax.jcr.Session} interface that returns the ModeShape-specific extension
 * interfaces from {@link #getWorkspace()}, {@link #getRepository()}, and {@link #getValueFactory()}.
 */
public interface Session extends javax.jcr.Session {

    @Override
    public Workspace getWorkspace();

    @Override
    public Repository getRepository();

    @Override
    public ValueFactory getValueFactory() throws RepositoryException;

    /**
     * Sequence the specified property using the named sequencer, and place the generated output at the specified location using
     * this session. The output nodes will be transient within the current session, so this session will need to be saved to
     * persist the output.
     * <p>
     * It is suggested that this method be used on inputs that the sequencer is not configured to process automatically, otherwise
     * ModeShape will also sequence the same property. If your application is to only manually sequence, simply configure each
     * sequencer without a path expression or with a path expression that will never apply to the manually-sequenced nodes.
     * </p>
     * 
     * @param sequencerName the name of the configured sequencer that should be executed
     * @param inputProperty the property that was changed and that should be used as the input; never null
     * @param outputNode the node that represents the output for the derived information; never null, and will either be
     *        {@link Node#isNew() new} if the output is being placed outside of the selected node, or will not be new when the
     *        output is to be placed on the selected input node
     * @return true if the sequencer did generate output; false if the sequencer could not process the specified content or if
     *         {@code sequencerName} is null or does not match a configured sequencer
     * @throws RepositoryException if there was a problem with the sequencer
     */
    boolean sequence( String sequencerName,
                      Property inputProperty,
                      Node outputNode ) throws RepositoryException;

    /**
     * Evaluate a local name and replace any characters that are not allowed within the local names of nodes and properties. Such
     * characters include '/', ':', '[', ']', '*', and '|', since these are all important to the rules for qualified names and
     * paths. When such characters are to be used within a <i>local name</i>, the application must escape them using this method
     * before the local name is used.
     * 
     * @param localName the local name to be encoded; can be <code>null</code> or empty
     * @return the supplied local name if it contains no illegal characters, or the encoded form of the supplied local name with
     *         all illegal characters replaced, or <code>null</code> if the input was <code>null</code>
     * @see #move(String, String)
     * @see javax.jcr.Node#addNode(String)
     * @see javax.jcr.Node#addNode(String, String)
     */
    String encode( final String localName );

    /**
     * Evaluate a local name and replace any characters that were previously {@link #encode(String) encoded}.
     * 
     * @param localName the local name to be decoded; can be <code>null</code> or empty
     * @return the supplied local name if it contains no encoded characters, or the decoded form of the supplied local name with
     *         all encoded characters replaced, or <code>null</code> if the input was <code>null</code>
     * @see #encode(String)
     */
    String decode( final String localName );

}
