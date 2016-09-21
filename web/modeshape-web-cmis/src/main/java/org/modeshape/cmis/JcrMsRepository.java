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

import javax.jcr.Repository;
import org.apache.chemistry.opencmis.commons.data.RepositoryInfo;
import org.apache.chemistry.opencmis.commons.enums.CmisVersion;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.RepositoryInfoImpl;
import org.apache.chemistry.opencmis.jcr.JcrRepository;
import org.apache.chemistry.opencmis.jcr.JcrTypeManager;
import org.apache.chemistry.opencmis.jcr.PathManager;
import org.apache.chemistry.opencmis.jcr.type.JcrTypeHandlerManager;

/**
 * ModeShape's extensions of OpenCMIS's {@link org.apache.chemistry.opencmis.jcr.JcrRepository}
 * 
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public class JcrMsRepository extends JcrRepository {
    
    public JcrMsRepository(Repository repository, PathManager pathManager,
                           JcrTypeManager typeManager,
                           JcrTypeHandlerManager typeHandlerManager) {
        super(repository, pathManager, typeManager, typeHandlerManager);
    }
    
    @Override
    protected RepositoryInfo compileRepositoryInfo(String repositoryId) {
        RepositoryInfo baseInfo = super.compileRepositoryInfo(repositoryId);
        RepositoryInfoImpl actualInfo = new RepositoryInfoImpl(baseInfo);
        // the default base class only advertises CMIS 1.0 as the supported version, while OpenChemistry supports 1.1 
        actualInfo.setCmisVersionSupported(CmisVersion.CMIS_1_1.value());
        return actualInfo;
    }
}
