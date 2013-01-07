/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
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

    private String repositoryId;
    private RepositoryInfo info;

    public RepositoryInfoImpl(String repositoryId, RepositoryInfo info) {
        this.repositoryId = repositoryId;
        this.info = info;
    }

    public String getId() {
        return repositoryId;
    }

    public String getName() {
        return info.getName();
    }

    public String getDescription() {
        return info.getDescription();
    }

    public String getVendorName() {
        return info.getVendorName();
    }

    public String getProductName() {
        return info.getProductName();
    }

    public String getProductVersion() {
        return info.getProductVersion();
    }

    public String getRootFolderId() {
        return info.getRootFolderId();
    }

    public RepositoryCapabilities getCapabilities() {
        return info.getCapabilities();
    }

    public AclCapabilities getAclCapabilities() {
        return info.getAclCapabilities();
    }

    public String getLatestChangeLogToken() {
        return info.getLatestChangeLogToken();
    }

    public String getCmisVersionSupported() {
        return info.getCmisVersionSupported();
    }

    public String getThinClientUri() {
        return info.getThinClientUri();
    }

    public Boolean getChangesIncomplete() {
        return info.getChangesIncomplete();
    }

    public List<BaseTypeId> getChangesOnType() {
        return info.getChangesOnType();
    }

    public String getPrincipalIdAnonymous() {
        return info.getPrincipalIdAnonymous();
    }

    public String getPrincipalIdAnyone() {
        return info.getPrincipalIdAnyone();
    }

    public List<CmisExtensionElement> getExtensions() {
        return info.getExtensions();
    }

    public void setExtensions(List<CmisExtensionElement> list) {
        info.setExtensions(list);
    }

}
