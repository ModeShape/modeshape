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
import org.apache.chemistry.opencmis.commons.data.ExtensionFeature;
import org.apache.chemistry.opencmis.commons.data.RepositoryCapabilities;
import org.apache.chemistry.opencmis.commons.data.RepositoryInfo;
import org.apache.chemistry.opencmis.commons.enums.BaseTypeId;
import org.apache.chemistry.opencmis.commons.enums.CmisVersion;

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

    @Override
    public String getId() {
        return repositoryId;
    }

    @Override
    public String getName() {
        return info.getName();
    }

    @Override
    public String getDescription() {
        return info.getDescription();
    }

    @Override
    public String getVendorName() {
        return info.getVendorName();
    }

    @Override
    public String getProductName() {
        return info.getProductName();
    }

    @Override
    public String getProductVersion() {
        return info.getProductVersion();
    }

    @Override
    public String getRootFolderId() {
        return info.getRootFolderId();
    }

    @Override
    public RepositoryCapabilities getCapabilities() {
        return info.getCapabilities();
    }

    @Override
    public AclCapabilities getAclCapabilities() {
        return info.getAclCapabilities();
    }

    @Override
    public String getLatestChangeLogToken() {
        return info.getLatestChangeLogToken();
    }

    @Override
    public String getCmisVersionSupported() {
        return info.getCmisVersionSupported();
    }

    @Override
    public String getThinClientUri() {
        return info.getThinClientUri();
    }

    @Override
    public Boolean getChangesIncomplete() {
        return info.getChangesIncomplete();
    }

    @Override
    public List<BaseTypeId> getChangesOnType() {
        return info.getChangesOnType();
    }

    @Override
    public String getPrincipalIdAnonymous() {
        return info.getPrincipalIdAnonymous();
    }

    @Override
    public String getPrincipalIdAnyone() {
        return info.getPrincipalIdAnyone();
    }

    @Override
    public List<CmisExtensionElement> getExtensions() {
        return info.getExtensions();
    }

    @Override
    public void setExtensions(List<CmisExtensionElement> list) {
        info.setExtensions(list);
    }

    @Override
    public CmisVersion getCmisVersion() {
        return info.getCmisVersion();
    }

    @Override
    public List<ExtensionFeature> getExtensionFeatures() {
        return info.getExtensionFeatures();
    }

}
