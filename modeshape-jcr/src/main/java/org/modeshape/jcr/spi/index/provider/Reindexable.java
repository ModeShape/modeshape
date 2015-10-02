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
package org.modeshape.jcr.spi.index.provider;

/**
 * Interface which defines the reindexing capabilities of an index.
 * 
 * @author Horia Chiorean (hchiorea@redhat.com)
 * @since 4.5
 */
public interface Reindexable {
    
    /**
     * Return whether this index is newly created and requires reindexing, or false if the index already exists.
     *
     * @return true if the index is new and needs to be rebuilt, or false otherwise.
     */
    boolean requiresReindexing();

    /**
     * Remove all of the index entries from the index. This is typically called prior to reindexing.
     */
    void clearAllData();
}
