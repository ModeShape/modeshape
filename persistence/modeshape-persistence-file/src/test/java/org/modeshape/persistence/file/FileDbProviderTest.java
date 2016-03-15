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
package org.modeshape.persistence.file;

import org.junit.Assert;
import org.junit.Test;
import org.modeshape.schematic.internal.document.BasicDocument;

/**
 * Unit test for {@link FileDbProvider}
 * 
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public class FileDbProviderTest {
    
    private FileDbProvider provider = new FileDbProvider();
    
    @Test
    public void shouldReturnDBBasedOnType() {
        Assert.assertNotNull(provider.getDB(FileDbProvider.TYPE_MEM, new BasicDocument()));        
        Assert.assertNotNull(provider.getDB(FileDbProvider.TYPE_FILE, new BasicDocument(FileDbProvider.PATH_FIELD, "path")));        
    }
    
    @Test(expected = NullPointerException.class)
    public void shouldFailIfPathNotProvided() throws Exception {
        provider.getDB(FileDbProvider.TYPE_FILE, new BasicDocument());        
    }
}
