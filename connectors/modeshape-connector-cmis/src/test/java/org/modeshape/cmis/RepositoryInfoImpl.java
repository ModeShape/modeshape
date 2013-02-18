/*
 * ModeShape (http://www.modeshape.org)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of
 * individual contributors.
 *
 * Unless otherwise indicated, all code in ModeShape is licensed
 * to you under the terms of the GNU Lesser General Public License as
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
package org.modeshape.cmis;

import java.util.List;
import org.apache.chemistry.opencmis.commons.data.AclCapabilities;
import org.apache.chemistry.opencmis.commons.data.CmisExtensionElement;
import org.apache.chemistry.opencmis.commons.data.RepositoryCapabilities;
import org.apache.chemistry.opencmis.commons.data.RepositoryInfo;
import org.apache.chemistry.opencmis.commons.enums.BaseTypeId;

/**
 *
 * @author kulikov
 */
public class RepositoryInfoImpl implements RepositoryInfo {

    @Override
    public String getId() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getName() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getDescription() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getVendorName() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getProductName() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getProductVersion() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getRootFolderId() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public RepositoryCapabilities getCapabilities() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public AclCapabilities getAclCapabilities() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getLatestChangeLogToken() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getCmisVersionSupported() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getThinClientUri() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Boolean getChangesIncomplete() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<BaseTypeId> getChangesOnType() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getPrincipalIdAnonymous() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getPrincipalIdAnyone() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<CmisExtensionElement> getExtensions() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setExtensions(List<CmisExtensionElement> list) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
