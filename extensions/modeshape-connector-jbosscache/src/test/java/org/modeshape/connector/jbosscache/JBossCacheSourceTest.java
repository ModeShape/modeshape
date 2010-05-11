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
package org.modeshape.connector.jbosscache;

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
import org.jboss.cache.Cache;
import org.jboss.cache.CacheFactory;
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
 * @author Randall Hauch
 */
public class JBossCacheSourceTest {

    private JBossCacheSource source;
    private RepositoryConnection connection;
    private String validName;
    private String validCacheConfigurationName;
    private String validCacheFactoryJndiName;
    private String validCacheJndiName;
    private UUID validRootNodeUuid;
    private ExecutionContext context;
    @Mock
    private Context jndiContext;
    @Mock
    private CacheFactory<Name, Object> cacheFactory;
    @Mock
    private Cache<Name, Object> cache;
    @Mock
    private RepositoryContext repositoryContext;

    @Before
    public void beforeEach() throws Exception {
        MockitoAnnotations.initMocks(this);
        context = new ExecutionContext();
        validName = "cache source";
        validCacheConfigurationName = "cache config name";
        validCacheFactoryJndiName = "cache factory jndi name";
        validCacheJndiName = "cache jndi name";
        validRootNodeUuid = UUID.randomUUID();
        source = new JBossCacheSource();

        // Set up the fake JNDI context ...
        source.setContext(jndiContext);
        source.initialize(repositoryContext);
        when(repositoryContext.getExecutionContext()).thenReturn(context);
        when(jndiContext.lookup(validCacheFactoryJndiName)).thenReturn(cacheFactory);
        when(jndiContext.lookup(validCacheJndiName)).thenReturn(cache);
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
        source = new JBossCacheSource();
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
        assertThat(source.getRetryLimit(), is(JBossCacheSource.DEFAULT_RETRY_LIMIT));
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
                                      validCacheConfigurationName,
                                      validCacheJndiName,
                                      validCacheFactoryJndiName,
                                      cachePolicy,
                                      100);
    }

    @Test
    public void shouldCreateJndiReferenceAndRecreatedObjectFromReferenceWithNullProperties() throws Exception {
        BasicCachePolicy cachePolicy = new BasicCachePolicy();
        cachePolicy.setTimeToLive(1000L, TimeUnit.MILLISECONDS);
        convertToAndFromJndiReference("some source", null, null, null, null, null, 100);
        convertToAndFromJndiReference(null, null, null, null, null, null, 100);
    }

    private void convertToAndFromJndiReference( String sourceName,
                                                UUID rootNodeUuid,
                                                String cacheConfigName,
                                                String cacheJndiName,
                                                String cacheFactoryJndiName,
                                                BasicCachePolicy cachePolicy,
                                                int retryLimit ) throws Exception {
        source.setRetryLimit(retryLimit);
        source.setName(sourceName);
        source.setCacheConfigurationName(cacheConfigName);
        source.setCacheFactoryJndiName(cacheFactoryJndiName);
        source.setCacheJndiName(cacheJndiName);
        source.setDefaultCachePolicy(cachePolicy);
        source.setRootNodeUuid(rootNodeUuid != null ? rootNodeUuid.toString() : null);

        Reference ref = source.getReference();
        assertThat(ref.getClassName(), is(JBossCacheSource.class.getName()));
        assertThat(ref.getFactoryClassName(), is(JBossCacheSource.class.getName()));

        Map<String, Object> refAttributes = new HashMap<String, Object>();
        Enumeration<RefAddr> enumeration = ref.getAll();
        while (enumeration.hasMoreElements()) {
            RefAddr addr = enumeration.nextElement();
            refAttributes.put(addr.getType(), addr.getContent());
        }

        // Recreate the object, use a newly constructed source ...
        ObjectFactory factory = new JBossCacheSource();
        Name name = mock(Name.class);
        Context context = mock(Context.class);
        Hashtable<?, ?> env = new Hashtable<Object, Object>();
        JBossCacheSource recoveredSource = (JBossCacheSource)factory.getObjectInstance(ref, name, context, env);
        assertThat(recoveredSource, is(notNullValue()));

        assertThat(recoveredSource.getName(), is(source.getName()));
        assertThat(recoveredSource.getRootNodeUuid(), is(source.getRootNodeUuid()));
        assertThat(recoveredSource.getCacheJndiName(), is(source.getCacheJndiName()));
        assertThat(recoveredSource.getRetryLimit(), is(source.getRetryLimit()));
        assertThat(recoveredSource.getCacheFactoryJndiName(), is(source.getCacheFactoryJndiName()));
        assertThat(recoveredSource.getCacheConfigurationName(), is(source.getCacheConfigurationName()));
        assertThat(recoveredSource.getDefaultCachePolicy(), is(source.getDefaultCachePolicy()));

        assertThat(recoveredSource.equals(source), is(true));
        assertThat(source.equals(recoveredSource), is(true));
    }

    @Test
    public void shouldCreateCacheUsingDefaultCacheFactoryWhenNoCacheOrCacheFactoryOrCacheConfigurationNameIsFound()
        throws Exception {
        source.setName(validName);
        connection = source.getConnection();
        assertThat(connection, is(notNullValue()));
        // assertThat(connection.getCache(), is(notNullValue()));
    }

    @Test
    public void shouldCreateCacheUsingDefaultCacheFactoryWithConfigurationNameWhenNoCacheOrCacheFactoryIsFound() throws Exception {

    }

    @Test
    public void shouldCreateCacheUsingCacheFactoryAndDefaultConfigurationWhenNoCacheOrCacheConfigurationNameIsFound()
        throws Exception {

    }

    @Test
    public void shouldCreateCacheUsingCacheFactoryAndConfigurationWhenNoCacheIsFound() throws Exception {

    }

    @Test
    public void shouldUseCacheIfFoundInJndi() throws Exception {

    }
}
