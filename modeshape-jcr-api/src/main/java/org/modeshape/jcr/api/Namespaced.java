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

import javax.jcr.RepositoryException;

/**
 * An orthogonal interface that defines methods to obtain the local part and namespace URI of an object that has a namespaced
 * name.
 */
public interface Namespaced {

    /**
     * Return the local part of the object's name.
     * 
     * @return the local part of the name, or an empty string if this Item is the root node of the workspace.
     * @throws RepositoryException if an error occurs.
     */
    public String getLocalName() throws RepositoryException;

    /**
     * Returns the URI in which this object's name is scoped. For example, if this object's JCR name is "jcr:primaryType", the
     * namespace prefix used in the name is "jcr", and so this method would return the "http://www.jcp.org/jcr/1.0" URI.
     * 
     * @return the URI of the namespace, or an empty string if the name does not use a namespace
     * @throws RepositoryException if an error occurs.
     */
    public String getNamespaceURI() throws RepositoryException;
}
