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
package org.modeshape.jcr;

import org.modeshape.jcr.cache.ChildReferences;
import org.modeshape.jcr.cache.MutableCachedNode;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.cache.RepositoryCache;
import org.modeshape.jcr.cache.SessionCache;
import org.modeshape.jcr.value.PropertyFactory;

/**
 * Class that performs internal initializations which may be required after a repository restore operation has completed. 
 * 
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
class RestoreContentInitializer implements RepositoryCache.ContentInitializer {
    
    @Override
    public void initialize( SessionCache systemSession, MutableCachedNode parent ) {
        PropertyFactory propertyFactory  = systemSession.getContext().getPropertyFactory();

        NodeKey systemNodeKey =  systemSession.getRootKey().withId(SystemContentInitializer.SYSTEM_NODE_ID);
        MutableCachedNode systemNode = systemSession.mutable(systemNodeKey);
        // the system node should always be there after a restore
        assert systemNode != null;

        // if we restored a pre 4.x repository, this will not have the mode:indexes node so we need to make sure it exists
        NodeKey indexesNodeKey = systemSession.getRootKey().withId(SystemContentInitializer.INDEXES_NODE_ID);
        ChildReferences children = systemNode.getChildReferences(systemSession);
        if (!children.hasChild(indexesNodeKey)) {
            systemNode.createChild(systemSession, indexesNodeKey, ModeShapeLexicon.INDEXES,
                                   propertyFactory.create(JcrLexicon.PRIMARY_TYPE));
        }
    }
}
