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
package org.modeshape.web.shared;

import java.io.Serializable;

/**
 *
 * @author kulikov
 */
public class BackupParams implements Serializable {
    private boolean includeBinaries;
    private boolean compress;
    private long documentsPerFile;
    
    public BackupParams() {
        
    }

    public boolean isIncludeBinaries() {
        return includeBinaries;
    }

    public void setIncludeBinaries(boolean includeBinaries) {
        this.includeBinaries = includeBinaries;
    }

    public long getDocumentsPerFile() {
        return documentsPerFile;
    }

    public void setDocumentsPerFile(long documentsPerFile) {
        this.documentsPerFile = documentsPerFile;
    }

    public boolean isCompress() {
        return compress;
    }

    public void setCompress(boolean compress) {
        this.compress = compress;
    }
    
}
