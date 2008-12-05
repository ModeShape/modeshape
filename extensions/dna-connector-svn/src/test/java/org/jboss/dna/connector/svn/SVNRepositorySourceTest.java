/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors. 
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.connector.svn;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.mock;
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
import org.jboss.dna.graph.cache.BasicCachePolicy;
import org.jboss.dna.graph.connectors.RepositoryConnection;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;

/**
 * @author Serge Pagop
 */
public class SVNRepositorySourceTest {

    private SVNRepositorySource source;
    private RepositoryConnection connection;
    private String validName;
    private String validUuidPropertyName;
    // private String validFileURL;
    private String validHttpURL;
    private String validHttpURLUsername;
    private String validHttpURLPassword;
    private UUID validRootNodeUuid;

    @Before
    public void beforeEach() throws Exception {
        MockitoAnnotations.initMocks(this);
        validName = "svn source";
        validUuidPropertyName = "dna:uuid";

        // For file protocol access
        // validFileURL = "file:///Users/sp/SVNRepos/test";

        // For http protocol access
        validHttpURL = "http://anonsvn.jboss.org/repos/dna/trunk/extensions/dna-connector-svn/src/test/resources";
        validHttpURLUsername = "anonymous";
        validHttpURLPassword = "anonymous";

        // For https protocol access
        // For svn protocol access
        // For svn-ssh protocol access

        validRootNodeUuid = UUID.randomUUID();
        source = new SVNRepositorySource();

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
    public void shouldNotSupportSameNameSiblings() {
        assertThat(source.getCapabilities().supportsSameNameSiblings(), is(false));
    }

    @Test
    public void shouldSupportUpdates() {
        assertThat(source.getCapabilities().supportsUpdates(), is(true));
    }

    @Test
    public void shouldHaveNullSourceNameUponConstruction() {
        assertThat(source.getName(), is(nullValue()));
    }

    @Test
    public void shouldAllowSettingName() {
        source.setName("name you like");
        assertThat(source.getName(), is("name you like"));
        source.setName("name you do not like");
        assertThat(source.getName(), is("name you do not like"));
    }

    @Test
    public void shouldAllowSettingNameToNull() {
        source.setName("something that can change the world");
        source.setName(null);
        assertThat(source.getName(), is(isNull()));
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowNullSVNUrl() {
        source.setSVNURL(null);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowEmptySVNUrl() {
        source.setSVNURL("");
    }

    @Test
    public void shouldAllowSettingEmptyCredentialsForAnnonymousAccess() {
        source.setSVNUsername("");
        assertThat(source.getSVNUsername(), is(notNullValue()));
        source.setSVNPassword("");
        assertThat(source.getSVNPassword(), is(notNullValue()));

    }

    @Test
    public void shouldHaveDefaultRetryLimit() {
        assertThat(source.getRetryLimit(), is(SVNRepositorySource.DEFAULT_RETRY_LIMIT));
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
                                      validHttpURL,
                                      validHttpURLUsername,
                                      validHttpURLPassword,
                                      validUuidPropertyName,
                                      cachePolicy,
                                      100);
    }

    @Test
    public void shouldCreateJndiReferenceAndRecreatedObjectFromReferenceWithNullProperties() throws Exception {
        BasicCachePolicy cachePolicy = new BasicCachePolicy();
        cachePolicy.setTimeToLive(1000L, TimeUnit.MILLISECONDS);
        convertToAndFromJndiReference("some source", null, "url1", null, null, null, null, 100);
        convertToAndFromJndiReference(null, null, "url2", null, null, null, null, 100);
    }

    private void convertToAndFromJndiReference( String sourceName,
                                                UUID rootNodeUuid,
                                                String url,
                                                String username,
                                                String password,
                                                String uuidPropertyName,
                                                BasicCachePolicy cachePolicy,
                                                int retryLimit ) throws Exception {
        source.setRetryLimit(retryLimit);
        source.setName(sourceName);
        source.setSVNURL(url);
        source.setSVNUsername(username);
        source.setSVNPassword(password);
        source.setDefaultCachePolicy(cachePolicy);

        Reference ref = source.getReference();

        assertThat(ref.getClassName(), is(SVNRepositorySource.class.getName()));
        assertThat(ref.getFactoryClassName(), is(SVNRepositorySource.class.getName()));

        Map<String, Object> refAttributes = new HashMap<String, Object>();
        Enumeration<RefAddr> enumeration = ref.getAll();
        while (enumeration.hasMoreElements()) {
            RefAddr addr = enumeration.nextElement();
            refAttributes.put(addr.getType(), addr.getContent());
        }

        assertThat((String)refAttributes.remove(SVNRepositorySource.SOURCE_NAME), is(source.getName()));
        assertThat((String)refAttributes.remove(SVNRepositorySource.SVN_URL), is(source.getSVNURL()));
        assertThat((String)refAttributes.remove(SVNRepositorySource.SVN_USERNAME), is(source.getSVNUsername()));
        assertThat((String)refAttributes.remove(SVNRepositorySource.SVN_PASSWORD), is(source.getSVNPassword()));
        assertThat((String)refAttributes.remove(SVNRepositorySource.RETRY_LIMIT), is(Integer.toString(source.getRetryLimit())));
        refAttributes.remove(SVNRepositorySource.DEFAULT_CACHE_POLICY);
        assertThat(refAttributes.isEmpty(), is(true));

        // Recreate the object, use a newly constructed source ...
        ObjectFactory factory = new SVNRepositorySource();
        Name name = mock(Name.class);
        Context context = mock(Context.class);
        Hashtable<?, ?> env = new Hashtable<Object, Object>();
        SVNRepositorySource recoveredSource = (SVNRepositorySource)factory.getObjectInstance(ref, name, context, env);
        assertThat(recoveredSource, is(notNullValue()));

        assertThat(recoveredSource.getName(), is(source.getName()));
        assertThat(recoveredSource.getSVNURL(), is(source.getSVNURL()));
        assertThat(recoveredSource.getSVNUsername(), is(source.getSVNUsername()));
        assertThat(recoveredSource.getSVNPassword(), is(source.getSVNPassword()));
        assertThat(recoveredSource.getDefaultCachePolicy(), is(source.getDefaultCachePolicy()));

        assertThat(recoveredSource.equals(source), is(true));
        assertThat(source.equals(recoveredSource), is(true));
    }

    // Only with local file protocol
    /*
    @Test
    public void shouldCreateFSRepositoryIfProtocolIsOfTypeFile() throws Exception {
        this.source.setName(validName);
        this.source.setSVNURL(validFileURL);
        this.connection = source.getConnection();
        assertThat(this.connection, is(notNullValue()));
    }*/

    @Test
    public void shouldCreateDAVRepositoryIfProtocolIsOfTypeHttp() throws Exception {
        this.source.setName(validName);
        this.source.setSVNURL(validHttpURL);
        this.source.setSVNUsername(validHttpURLUsername);
        this.source.setSVNPassword(validHttpURLPassword);
        this.connection = source.getConnection();
        assertThat(this.connection, is(notNullValue()));
    }

}
