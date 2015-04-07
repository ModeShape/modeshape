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

/**
 * Class which allows a customization of the restore process
 * 
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public abstract class RestoreOptions {
    
    /**
     * The default options used during restore, if no explicit ones are given.
     */
    public static final RestoreOptions DEFAULT = new RestoreOptions(){};

    /**
     * Whether a full reindexing should be performed or not after restoring the content.
     * 
     * @return {@code true} if a full reindexing should be performed; defaults to {@code true}
     */
    public boolean reindexContentOnFinish() {
        return true;
    }

    /**
     * Whether binaries should be restored or not. ModeShape uses references between documents and binary values, so 
     * depending on the context it may not always be desired for binary values to be restored. 
     * 
     * @return {@code true} if binary values should be included; defaults to {@code true}
     */
    public boolean includeBinaries() {
        return true;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("[restore_options: ");
        builder.append("include_binaries=").append(includeBinaries());
        builder.append(", reindex_content_on_finish=").append(reindexContentOnFinish());
        builder.append("]");
        return builder.toString();
    }
}
