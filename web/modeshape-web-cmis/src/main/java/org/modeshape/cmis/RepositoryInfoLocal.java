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

import org.apache.chemistry.opencmis.commons.data.RepositoryInfo;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.RepositoryInfoImpl;

/**
 * This class overrides identifier of the repository. To be able to manipulate with multiple repositories this class changes the
 * repository identifier to follow composite naming rules.
 * 
 * @author kulikov
 */
public class RepositoryInfoLocal extends RepositoryInfoImpl {
    private static final long serialVersionUID = 1L;
    private String id;

    /**
     * Creates new data object.
     * 
     * @param id the new repository identifier.
     * @param info the original repository info object
     */
    public RepositoryInfoLocal( String id,
                                RepositoryInfo info ) {
        super(info);
        this.id = id;
    }

    @Override
    public String getId() {
        return id;
    }
}
