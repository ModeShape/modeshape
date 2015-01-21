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

import java.util.Iterator;
import java.util.Set;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.version.Version;
import javax.jcr.version.VersionException;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionIterator;
import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.exceptions.CmisObjectNotFoundException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisRuntimeException;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertiesImpl;
import org.apache.chemistry.opencmis.commons.impl.server.ObjectInfoImpl;
import org.apache.chemistry.opencmis.jcr.JcrTypeManager;
import org.apache.chemistry.opencmis.jcr.JcrVersion;
import org.apache.chemistry.opencmis.jcr.PathManager;
import org.apache.chemistry.opencmis.jcr.type.JcrTypeHandlerManager;
import org.modeshape.jcr.api.Binary;

/**
 * Modified
 * <code>JcrVersion</code> class.
 *
 * @author kulikov
 */
public class JcrMsVersion extends JcrVersion {

    private static final String HASH_ALGORITHM = "sha-1";

    public JcrMsVersion(Node node, Version version, JcrTypeManager typeManager, PathManager pathManager,
            JcrTypeHandlerManager typeHandlerManager) {

        super(node, version, typeManager, pathManager, typeHandlerManager);
    }

    @Override
    public Iterator<JcrVersion> getVersions() {
        try {
            VersionHistory versionHistory = getVersionHistory(getNode());
            final VersionIterator versions = versionHistory.getAllLinearVersions();

            return new Iterator<JcrVersion>() {
                @Override
                public boolean hasNext() {
                    return versions.hasNext();
                }

                @Override
                public JcrVersion next() {
                    return new JcrMsVersionNode(getNode(), versions.nextVersion(), typeManager, pathManager,
                            typeHandlerManager);
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        } catch (RepositoryException e) {
            throw new CmisRuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public JcrVersion getVersion(String name) {
        try {
            Node node = getNode();
            VersionHistory versionHistory = getVersionHistory(node);
            Version version = versionHistory.getVersion(name);
            return new JcrMsVersion(node, version, typeManager, pathManager, typeHandlerManager);
        } catch (UnsupportedRepositoryOperationException | VersionException e) {
            throw new CmisObjectNotFoundException(e.getMessage(), e);
        } catch (RepositoryException e) {
            throw new CmisRuntimeException(e.getMessage(), e);
        }
    }

    @Override
    protected void compileProperties(PropertiesImpl properties, Set<String> filter, ObjectInfoImpl objectInfo)
            throws RepositoryException {
        super.compileProperties(properties, filter, objectInfo);
        //append cmis:contentStreamHash  property
        addPropertyString(properties, getTypeIdInternal(), filter, PropertyIds.CONTENT_STREAM_HASH, getHash());
    }

    /**
     * Get the hexadecimal form of the SHA-1 hash of the contents. 
     * 
     * @return the hexadecimal form of hash, or a null string if the hash could not be computed or is not known
     */
    private String getHash() {
        try {
            Node contentNode = getContextNode();
            Property data = contentNode.getProperty(Property.JCR_DATA);
            Binary bin = (Binary) data.getBinary();
            return String.format("{%s}%s", HASH_ALGORITHM, bin.getHexHash());
        } catch (RepositoryException e) {
            return "";
        }
    }
}
