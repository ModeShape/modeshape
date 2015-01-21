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
package org.modeshape.cmis;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.version.Version;
import javax.jcr.version.VersionManager;
import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.definitions.TypeDefinition;
import org.apache.chemistry.opencmis.commons.enums.Cardinality;
import org.apache.chemistry.opencmis.commons.enums.PropertyType;
import org.apache.chemistry.opencmis.commons.enums.Updatability;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.AbstractTypeDefinition;
import org.apache.chemistry.opencmis.jcr.JcrDocument;
import org.apache.chemistry.opencmis.jcr.JcrTypeManager;
import org.apache.chemistry.opencmis.jcr.impl.DefaultDocumentTypeHandler;

/**
 *
 * @author kulikov
 */
public class MsDocumentTypeHandler extends DefaultDocumentTypeHandler {
    
    @Override
    public TypeDefinition getTypeDefinition() {
        AbstractTypeDefinition def = (AbstractTypeDefinition)super.getTypeDefinition();
        //append cmis:contentStreamHash property definition
        def.addPropertyDefinition(JcrTypeManager.createPropDef(
                PropertyIds.CONTENT_STREAM_HASH, 
                "Content Stream Hash", 
                "Content Stream Hash",
                PropertyType.STRING, 
                Cardinality.SINGLE, 
                Updatability.READONLY, 
                false, 
                true));
        return def;
    }
    
    @Override
    public JcrDocument getJcrNode(Node node) throws RepositoryException {
        if (node.isNodeType(NodeType.NT_VERSION)) {
            return new JcrMsVersionNode(node, (Version) node, typeManager, pathManager, typeHandlerManager);
        }
        VersionManager versionManager = node.getSession().getWorkspace().getVersionManager();
        Version version = versionManager.getBaseVersion(node.getPath());
        return new JcrMsVersion(node, version, typeManager, pathManager, typeHandlerManager);
    }

    @Override
    public boolean canHandle(Node node) throws RepositoryException {
        return super.canHandle(node) || node.isNodeType(NodeType.NT_VERSION);
    }
}
