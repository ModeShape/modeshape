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
package org.modeshape.jcr.value.basic;

import javax.jcr.Node;
import org.modeshape.common.text.TextDecoder;
import org.modeshape.jcr.JcrSession;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.value.Reference;
import org.modeshape.jcr.value.ReferenceFactory;
import org.modeshape.jcr.value.ValueFactories;

/**
 * A custom ReferenceValueFactory specialization that knows about a particular workspace, used to handle conversion from
 * {@link Node#getIdentifier()} strings, including those that are local (e.g., not
 * {@link JcrSession#isForeignKey(NodeKey, NodeKey) foreign}) and thus don't have the source part and workspace part.
 * 
 * @see JcrSession
 */
public class NodeIdentifierReferenceFactory extends ReferenceValueFactory {

    private final NodeKey rootKey;

    /**
     * @param rootKey
     * @param decoder
     * @param factories
     * @param weak
     */
    public NodeIdentifierReferenceFactory( NodeKey rootKey,
                                           TextDecoder decoder,
                                           ValueFactories factories,
                                           boolean weak ) {
        super(decoder, factories, weak);
        this.rootKey = rootKey;
    }

    @Override
    public Reference create( String value ) {
        if (value == null) return null;
        NodeKey key = JcrSession.createNodeKeyFromIdentifier(value, rootKey);
        boolean isForeign = !(key.getSourceKey().equals(rootKey.getSourceKey()) && key.getWorkspaceKey()
                                                                                      .equals(rootKey.getWorkspaceKey()));
        return new NodeKeyReference(key, weak, isForeign);
    }

    @Override
    public ReferenceFactory with( ValueFactories valueFactories ) {
        return new NodeIdentifierReferenceFactory(rootKey, decoder, valueFactories, weak);
    }
}
