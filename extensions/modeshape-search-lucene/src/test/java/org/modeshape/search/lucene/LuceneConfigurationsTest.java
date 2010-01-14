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

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertThat;
import java.io.File;
import java.util.Map;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.RAMDirectory;
import org.modeshape.common.text.FilenameEncoder;
import org.modeshape.common.util.FileUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

public class LuceneConfigurationsTest {

    private LuceneConfiguration config;
    private String workspace;
    private String index;
    private Directory directory;
    private Multimap<String, String> indexNamesByWorkspaceName;
    private File tempArea;

    @Before
    public void beforeEach() {
        workspace = "workspace";
        index = "index";
        indexNamesByWorkspaceName = HashMultimap.create();
        tempArea = new File("target/configTest");
        if (tempArea.exists()) FileUtil.delete(tempArea); // deletes recursively
        tempArea.mkdirs();
    }

    @After
    public void afterEach() {
        if (config != null) {
            try {
                for (Map.Entry<String, String> entry : indexNamesByWorkspaceName.entries()) {
                    assertThat(config.destroyDirectory(entry.getKey(), entry.getValue()), is(true));
                }
            } finally {
                config = null;
                directory = null;
                indexNamesByWorkspaceName.clear();
            }
        }
        if (tempArea != null) {
            try {
                FileUtil.delete(tempArea); // deletes recursively
            } finally {
                tempArea = null;
            }
        }
    }

    protected void destroyDirectory( LuceneConfiguration config,
                                     String workspaceName,
                                     String indexName ) {
        assertThat(config.destroyDirectory(workspace, index), is(true));
        indexNamesByWorkspaceName.remove(workspaceName, indexName);
    }

    protected Directory getDirectory( LuceneConfiguration config,
                                      String workspaceName,
                                      String indexName ) {
        Directory result = config.getDirectory(workspaceName, indexName);
        assertThat(result, is(notNullValue()));
        indexNamesByWorkspaceName.put(workspaceName, indexName);
        return result;
    }

    // ----------------------------------------------------------------------------------------------------------------
    // In-Memory directories ...
    // ----------------------------------------------------------------------------------------------------------------

    @Test
    public void shouldCreateConfigurationFromInMemoryStorage() {
        config = LuceneConfigurations.inMemory();
        assertThat(config, is(notNullValue()));
        directory = getDirectory(config, workspace, index);
        assertThat(directory, is(instanceOf(RAMDirectory.class)));
    }

    @Test
    public void shouldReturnSameDirectoryForSameWorkspaceAndIndexNamesFromInMemoryConfiguration() {
        config = LuceneConfigurations.inMemory();
        assertThat(config, is(notNullValue()));
        directory = getDirectory(config, workspace, index);
        assertThat(directory, is(instanceOf(RAMDirectory.class)));
        for (int i = 0; i != 10; ++i) {
            assertThat(getDirectory(config, workspace, index), is(sameInstance(directory)));
        }
        assertThat(indexNamesByWorkspaceName.size(), is(1));
    }

    // ----------------------------------------------------------------------------------------------------------------
    // FileSystem directories ...
    // ----------------------------------------------------------------------------------------------------------------

    @Test
    public void shouldCreateConfigurationFromFileSystemStorage() {
        config = LuceneConfigurations.using(tempArea);
        assertThat(config, is(notNullValue()));
        directory = getDirectory(config, workspace, index);
        assertThat(directory, is(instanceOf(FSDirectory.class)));
        FSDirectory fsDirectory = (FSDirectory)directory;
        assertThat(fsDirectory.getFile().getName(), is(index));
        assertThat(fsDirectory.getFile().getParentFile().getName(), is(workspace));
    }

    @Test
    public void shouldReturnSameDirectoryForSameWorkspaceAndIndexNamesFromFileSystemStorage() {
        config = LuceneConfigurations.using(tempArea);
        assertThat(config, is(notNullValue()));
        directory = getDirectory(config, workspace, index);
        assertThat(directory, is(instanceOf(FSDirectory.class)));
        FSDirectory fsDirectory = (FSDirectory)directory;
        assertThat(fsDirectory.getFile().getName(), is(index));
        assertThat(fsDirectory.getFile().getParentFile().getName(), is(workspace));
        for (int i = 0; i != 10; ++i) {
            assertThat(getDirectory(config, workspace, index), is(sameInstance(directory)));
        }
        assertThat(indexNamesByWorkspaceName.size(), is(1));
    }

    @Test
    public void shouldEncodeDirectoryNames() {
        // Set up an encoder and make sure that the names for the index and workspace can't be used as file system names ...
        FilenameEncoder encoder = new FilenameEncoder();
        index = "some/special::/\nindex(name)";
        workspace = "some/special::/\nworkspace(name)/illegalInWindows:\\/?%*|\"'<>.txt";
        assertThat(index, is(not(encoder.encode(index))));
        assertThat(workspace, is(not(encoder.encode(workspace))));

        config = LuceneConfigurations.using(tempArea, null, encoder, encoder);
        assertThat(config, is(notNullValue()));
        directory = getDirectory(config, workspace, index);
        assertThat(directory, is(instanceOf(FSDirectory.class)));
        FSDirectory fsDirectory = (FSDirectory)directory;
        assertThat(fsDirectory.getFile().getName(), is(encoder.encode(index)));
        assertThat(fsDirectory.getFile().getParentFile().getName(), is(encoder.encode(workspace)));
    }
}
