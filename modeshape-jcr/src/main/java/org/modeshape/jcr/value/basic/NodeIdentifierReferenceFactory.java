/*
 * ModeShape (http://www.modeshape.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.modeshape.jcr.value.basic;

import javax.jcr.Node;
import org.modeshape.common.text.TextDecoder;
import org.modeshape.jcr.JcrSession;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.value.PropertyType;
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


    public static NodeIdentifierReferenceFactory newInstance( NodeKey rootKey,
                                                              TextDecoder decoder,
                                                              ValueFactories factories,
                                                              boolean weak,
                                                              boolean simple ) {
        if (simple) {
            return new NodeIdentifierReferenceFactory(PropertyType.SIMPLEREFERENCE, decoder, factories, weak, simple, rootKey);
        }
        return new NodeIdentifierReferenceFactory(weak ? PropertyType.WEAKREFERENCE : PropertyType.REFERENCE, decoder,
                                                  factories,
                                                  weak,
                                                  simple,
                                                  rootKey);
    }

    protected NodeIdentifierReferenceFactory( PropertyType type,
                                              TextDecoder decoder,
                                              ValueFactories valueFactories,
                                              boolean weak,
                                              boolean simple,
                                              NodeKey rootKey ) {
        super(type, decoder, valueFactories, weak, simple);
        this.rootKey = rootKey;
    }

    @Override
    public Reference create( String value ) {
        if (value == null) return null;
        NodeKey key = JcrSession.createNodeKeyFromIdentifier(value, rootKey);
        boolean isForeign = !(key.getSourceKey().equals(rootKey.getSourceKey()) && key.getWorkspaceKey()
                                                                                      .equals(rootKey.getWorkspaceKey()));
        return new NodeKeyReference(key, weak, isForeign, simple);
    }

    @Override
    public ReferenceFactory with( ValueFactories valueFactories ) {
        return valueFactories == this.valueFactories ?
               this : new NodeIdentifierReferenceFactory(super.getPropertyType(), decoder, valueFactories, weak, simple, rootKey);
    }
}
