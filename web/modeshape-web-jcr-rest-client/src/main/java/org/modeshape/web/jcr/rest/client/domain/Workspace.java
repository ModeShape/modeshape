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
package org.modeshape.web.jcr.rest.client.domain;

import org.modeshape.common.annotation.Immutable;
import org.modeshape.common.util.HashCode;
import org.modeshape.web.jcr.rest.client.RestClientI18n;

/**
 * The <code>Workspace</code> class is the business object for a ModeShape repository workspace.
 */
@Immutable
public class Workspace implements IModeShapeObject {

    // ===========================================================================================================================
    // Fields
    // ===========================================================================================================================

    /**
     * The workspace name.
     */
    private final String name;

    /**
     * The repository where this workspace resides.
     */
    private final Repository repository;

    // ===========================================================================================================================
    // Constructors
    // ===========================================================================================================================

    /**
     * Constructs a new <code>Workspace</code>.
     * 
     * @param name the workspace name (never <code>null</code>)
     * @param repository the repository where this workspace resides (never <code>null</code>)
     * @throws IllegalArgumentException if any of the arguments are <code>null</code>
     */
    public Workspace( String name,
                      Repository repository ) {
        assert name != null;
        assert repository != null;
        this.name = name;
        this.repository = repository;
    }

    // ===========================================================================================================================
    // Methods
    // ===========================================================================================================================

    @Override
    public boolean equals( Object obj ) {
        if (this == obj) return true;
        if ((obj == null) || (getClass() != obj.getClass())) return false;

        Workspace otherWorkspace = (Workspace)obj;
        return (this.name.equals(otherWorkspace.name) && this.repository.equals(otherWorkspace.repository));
    }

    @Override
    public String getName() {
        return this.name;
    }

    /**
     * @return the repository where this workspace is located (never <code>null</code>)
     */
    public Repository getRepository() {
        return this.repository;
    }

    /**
     * @return the server where this workspace is located (never <code>null</code>)
     */
    public Server getServer() {
        return this.repository.getServer();
    }

    @Override
    public String getShortDescription() {
        return RestClientI18n.workspaceShortDescription.text(this.name, this.repository.getName());
    }

    @Override
    public int hashCode() {
        return HashCode.compute(this.name, this.repository);
    }

    @Override
    public String toString() {
        return getShortDescription();
    }

}
