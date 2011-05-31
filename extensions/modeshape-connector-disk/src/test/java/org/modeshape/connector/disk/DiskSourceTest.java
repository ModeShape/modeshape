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
package org.modeshape.connector.disk;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.spi.ObjectFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.cache.BasicCachePolicy;
import org.modeshape.graph.connector.RepositoryConnection;
import org.modeshape.graph.connector.RepositoryContext;

/**
 */
public class DiskSourceTest {

    private ExecutionContext context;
    private DiskSource source;
    private RepositoryConnection connection;
    private String validName;
    private String repositoryRootPath;
    private UUID validRootNodeUuid;
    @Mock
    private RepositoryContext repositoryContext;

    @Before
    public void beforeEach() throws Exception {
        MockitoAnnotations.initMocks(this);
        context = new ExecutionContext();
        when(repositoryContext.getExecutionContext()).thenReturn(context);
        validName = "disk source";
        validRootNodeUuid = UUID.randomUUID();
        repositoryRootPath = "target/root";
        source = new DiskSource();

        source.setRepositoryRootPath(repositoryRootPath);
    }

    @After
    public void afterEach() throws Exception {
        if (connection != null) {
            connection.close();
        }
    }

    @Test
    public void shouldReturnNonNullCapabilities() {
        assertThat(source.getCapabilities(), is(notNullValue()));
    }

    @Test
    public void shouldSupportSameNameSiblings() {
        assertThat(source.getCapabilities().supportsSameNameSiblings(), is(true));
    }

    @Test
    public void shouldSupportUpdates() {
        assertThat(source.getCapabilities().supportsUpdates(), is(true));
    }

    @Test
    public void shouldHaveNullSourceNameUponConstruction() {
        source = new DiskSource();
        assertThat(source.getName(), is(nullValue()));
    }

    @Test
    public void shouldAllowSettingName() {
        source.setName("Something");
        assertThat(source.getName(), is("Something"));
        source.setName("another name");
        assertThat(source.getName(), is("another name"));
    }

    @Test
    public void shouldAllowSettingNameToNull() {
        source.setName("some name");
        source.setName(null);
        assertThat(source.getName(), is(nullValue()));
    }

    @Test
    public void shouldHaveDefaultRetryLimit() {
        assertThat(source.getRetryLimit(), is(DiskSource.DEFAULT_RETRY_LIMIT));
    }

    @Test
    public void shouldSetRetryLimitToZeroWhenSetWithNonPositiveValue() {
        source.setRetryLimit(0);
        assertThat(source.getRetryLimit(), is(0));
        source.setRetryLimit(-1);
        assertThat(source.getRetryLimit(), is(0));
        source.setRetryLimit(-100);
        assertThat(source.getRetryLimit(), is(0));
    }

    @Test
    public void shouldAllowRetryLimitToBeSet() {
        for (int i = 0; i != 100; ++i) {
            source.setRetryLimit(i);
            assertThat(source.getRetryLimit(), is(i));
        }
    }

    @Test
    public void shouldCreateJndiReferenceAndRecreatedObjectFromReference() throws Exception {
        BasicCachePolicy cachePolicy = new BasicCachePolicy();
        cachePolicy.setTimeToLive(1000L, TimeUnit.MILLISECONDS);
        convertToAndFromJndiReference(validName,
                                      validRootNodeUuid,
 repositoryRootPath,
                                      cachePolicy,
                                      100);
    }

    @Test
    public void shouldCreateJndiReferenceAndRecreatedObjectFromReferenceWithNullProperties() throws Exception {
        BasicCachePolicy cachePolicy = new BasicCachePolicy();
        cachePolicy.setTimeToLive(1000L, TimeUnit.MILLISECONDS);
        convertToAndFromJndiReference("some source", null, null, null, 100);
        convertToAndFromJndiReference(null, null, null, null, 100);
    }

    private void convertToAndFromJndiReference( String sourceName,
                                                UUID rootNodeUuid,
                                                String repositoryRootPath,
                                                BasicCachePolicy cachePolicy,
                                                int retryLimit ) throws Exception {
        source.setRetryLimit(retryLimit);
        source.setName(sourceName);
        source.setDefaultCachePolicy(cachePolicy);
        source.setRootNodeUuid(rootNodeUuid != null ? rootNodeUuid.toString() : null);
        source.setRepositoryRootPath(repositoryRootPath);

        Reference ref = source.getReference();
        assertThat(ref.getClassName(), is(DiskSource.class.getName()));
        assertThat(ref.getFactoryClassName(), is(DiskSource.class.getName()));

        Map<String, Object> refAttributes = new HashMap<String, Object>();
        Enumeration<RefAddr> enumeration = ref.getAll();
        while (enumeration.hasMoreElements()) {
            RefAddr addr = enumeration.nextElement();
            refAttributes.put(addr.getType(), addr.getContent());
        }

        // Recreate the object, use a newly constructed source ...
        ObjectFactory factory = new DiskSource();
        Name name = mock(Name.class);
        Context context = mock(Context.class);
        Hashtable<?, ?> env = new Hashtable<Object, Object>();
        DiskSource recoveredSource = (DiskSource)factory.getObjectInstance(ref, name, context, env);
        assertThat(recoveredSource, is(notNullValue()));

        assertThat(recoveredSource.getName(), is(source.getName()));
        assertThat(recoveredSource.getRootNodeUuid(), is(source.getRootNodeUuid()));
        assertThat(recoveredSource.getRetryLimit(), is(source.getRetryLimit()));
        assertThat(recoveredSource.getRepositoryRootPath(), is(source.getRepositoryRootPath()));
        assertThat(recoveredSource.getDefaultCachePolicy(), is(source.getDefaultCachePolicy()));

        assertThat(recoveredSource.equals(source), is(true));
        assertThat(source.equals(recoveredSource), is(true));
    }

    @Test
    public void shouldCreateCacheUsingDefaultCacheManagerWhenNoCacheOrCacheManagerOrCacheConfigurationNameIsFound()
        throws Exception {
        source.setName(validName);
        source.initialize(repositoryContext);
        connection = source.getConnection();
        assertThat(connection, is(notNullValue()));
        // assertThat(connection.getCache(), is(notNullValue()));
    }
}
