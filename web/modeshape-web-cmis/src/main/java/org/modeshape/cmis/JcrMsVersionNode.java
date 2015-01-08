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

import java.util.HashSet;
import java.util.Set;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.version.Version;
import org.apache.chemistry.opencmis.commons.data.ObjectData;
import org.apache.chemistry.opencmis.commons.exceptions.CmisRuntimeException;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.ObjectDataImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertiesImpl;
import org.apache.chemistry.opencmis.commons.impl.server.ObjectInfoImpl;
import org.apache.chemistry.opencmis.commons.server.ObjectInfoHandler;
import org.apache.chemistry.opencmis.jcr.JcrTypeManager;
import org.apache.chemistry.opencmis.jcr.PathManager;
import org.apache.chemistry.opencmis.jcr.type.JcrTypeHandlerManager;

/**
 *
 * @author kulikov
 */
public class JcrMsVersionNode extends JcrMsVersion {

    private final Version version;

    public JcrMsVersionNode(Node node, Version version, JcrTypeManager typeManager, PathManager pathManager,
            JcrTypeHandlerManager typeHandlerManager) {

        super(node, version, typeManager, pathManager, typeHandlerManager);
        this.version = version;
    }

    @Override
    protected String getVersionSeriesId() {
        try {
            return version.getIdentifier();
        } catch (RepositoryException e) {
            return null;
        }
    }

    @Override
    protected boolean isLatestVersion() throws RepositoryException {
        return true;
    }

    @Override
    protected String getBaseNodeId() throws RepositoryException {
        return this.version.getIdentifier();
    }

    @Override
    public boolean isVersionable() {
        return false;
    }    
    /**
     * Compile the
     * <code>ObjectData</code> for this node
     */
    @Override
    public ObjectData compileObjectType(Set<String> filter, Boolean includeAllowableActions,
            ObjectInfoHandler objectInfos, boolean requiresObjectInfo) {

        try {
            ObjectDataImpl result = new ObjectDataImpl();
            ObjectInfoImpl objectInfo = new ObjectInfoImpl();

            PropertiesImpl properties = new PropertiesImpl();
            filter = filter == null ? null : new HashSet<>(filter);
            compileProperties(properties, filter, objectInfo);
            result.setProperties(properties);

            if (Boolean.TRUE.equals(includeAllowableActions)) {
                result.setAllowableActions(getAllowableActions());
            }

            if (requiresObjectInfo) {
                objectInfo.setObject(result);
                objectInfos.addObjectInfo(objectInfo);
            }

            return result;
        } catch (RepositoryException e) {
            throw new CmisRuntimeException(e.getMessage(), e);
        }
    }
}
