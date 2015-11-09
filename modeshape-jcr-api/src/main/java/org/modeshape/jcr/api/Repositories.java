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
package org.modeshape.jcr.api;

import java.util.Set;

/**
 * Representation of a generic container of multiple {@link javax.jcr.Repository Repository} instances. This interface helps
 * decouple applications that use JCR repositories from the actual JCR implementation.
 */
public interface Repositories {

    /**
     * Get the names of the available repositories.
     * 
     * @return the immutable set of repository names provided by this server; never null
     */
    Set<String> getRepositoryNames();

    /**
     * Return the JCR Repository with the supplied name.
     * 
     * @param repositoryName the name of the repository to return; may not be null
     * @return the repository with the given name; never null
     * @throws javax.jcr.RepositoryException if no repository exists with the given name or there is an error communicating with
     *         the repository
     */
    Repository getRepository( String repositoryName ) throws javax.jcr.RepositoryException;
}
