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

package org.modeshape.jcr.spi.federation;

import org.infinispan.schematic.document.Document;

/**
 * Interface that should be implemented by {@link Connector}(s) that want to expose children of
 * nodes in a "page by page" fashion. For effectively creating blocks of children for each page, connector implementations should
 * use the {@link PageWriter} extension.
 * 
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public interface Pageable {

    /**
     * Returns a document which represents the document of a parent node to which an optional page of children has been added.
     * In order to add a next page, {@link PageWriter#addPage(String, String, long, long)} should be used to add a
     * new page of children.
     * 
     * @param pageKey a {@code non-null} {@link PageKey} instance, which offers information about the page that should be
     *        retrieved.
     * @return a {@code non-null} document representing the parent document.
     */
    Document getChildren( PageKey pageKey );

}
