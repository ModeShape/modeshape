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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.common.util.HashCode;
import org.modeshape.web.jcr.rest.client.RestClientI18n;

/**
 * The Repository class is the business object for a ModeShape repository.
 */
@Immutable
public class Repository implements IModeShapeObject {

    // ===========================================================================================================================
    // Fields
    // ===========================================================================================================================

    /**
     * The repository name.
     */
    private final String name;

    /**
     * The server where this repository resides.
     */
    private final Server server;
    private final Map<String, Object> metadata;

    // ===========================================================================================================================
    // Constructors
    // ===========================================================================================================================

    /**
     * Constructs a new <code>Repository</code>.
     * 
     * @param name the repository name (never <code>null</code>)
     * @param server the server where this repository resides (never <code>null</code>)
     * @throws IllegalArgumentException if the name or server argument is <code>null</code>
     */
    public Repository( String name,
                       Server server ) {
        assert name != null;
        assert server != null;
        this.name = name;
        this.server = server;
        this.metadata = Collections.emptyMap();
    }

    /**
     * Constructs a new <code>Repository</code>.
     * 
     * @param name the repository name (never <code>null</code>)
     * @param server the server where this repository resides (never <code>null</code>)
     * @param metadata the metadata; may be null or empty
     * @throws IllegalArgumentException if the name or server argument is <code>null</code>
     */
    public Repository( String name,
                       Server server,
                       Map<String, Object> metadata ) {
        assert name != null;
        assert server != null;
        this.name = name;
        this.server = server;
        this.metadata = metadata != null ? Collections.unmodifiableMap(new HashMap<String, Object>(metadata)) : Collections.<String, Object>emptyMap();
    }

    // ===========================================================================================================================
    // Methods
    // ===========================================================================================================================

    @Override
    public boolean equals( Object obj ) {
        if (this == obj) return true;
        if ((obj == null) || (getClass() != obj.getClass())) return false;

        Repository otherRepository = (Repository)obj;
        return (this.name.equals(otherRepository.name) && this.server.equals(otherRepository.server));
    }

    @Override
    public String getName() {
        return this.name;
    }

    /**
     * @return the server where this repository is located (never <code>null</code>)
     */
    public Server getServer() {
        return this.server;
    }

    /**
     * Get the metadata for this repository. This metadata typically includes all of a JCR Repository's descriptors, where the
     * values are usually strings or arrays of strings.
     * 
     * @return the immutable map of metadata; never null but possibly empty if the metadata is not known
     */
    public Map<String, Object> getMetadata() {
        return metadata;
    }

    @Override
    public String getShortDescription() {
        return RestClientI18n.repositoryShortDescription.text(this.name, this.server.getName());
    }

    @Override
    public int hashCode() {
        return HashCode.compute(this.name, this.server);
    }

    @Override
    public String toString() {
        return getShortDescription();
    }

}
