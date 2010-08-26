/*
 * ModeShape (http://www.modeshape.org)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * ModeShape is free software. Unless otherwise indicated, all code in ModeShape
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * ModeShape is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.modeshape.search.lucene;

import net.jcip.annotations.ThreadSafe;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.Version;
import org.modeshape.graph.search.SearchEngineException;

/**
 * Interface used to obtain the Lucene {@link Directory} instance that should be used for a workspace given the name of the
 * workspace. There are several implementations (see {@link LuceneConfigurations}), but custom implementations can always be used.
 */
@ThreadSafe
public interface LuceneConfiguration {

    /**
     * Get the version for the Lucene configuration.
     * 
     * @return the version
     */
    Version getVersion();

    /**
     * Get the {@link Directory} that should be used for the workspace with the supplied name.
     * 
     * @param workspaceName the workspace name
     * @param indexName the name of the index to be created
     * @return the directory; never null
     * @throws IllegalArgumentException if the workspace name is null
     * @throws SearchEngineException if there is a problem creating the directory
     */
    Directory getDirectory( String workspaceName,
                            String indexName ) throws SearchEngineException;

    /**
     * Destroy the {@link Directory} that is used for the workspace with the supplied name.
     * 
     * @param workspaceName the workspace name
     * @param indexName the name of the index to be created
     * @return true if the directory existed and was destroyed, or false if the directory didn't exist
     * @throws IllegalArgumentException if the workspace name is null
     * @throws SearchEngineException if there is a problem creating the directory
     */
    boolean destroyDirectory( String workspaceName,
                              String indexName ) throws SearchEngineException;
}
