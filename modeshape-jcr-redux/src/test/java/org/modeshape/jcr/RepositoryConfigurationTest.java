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
package org.modeshape.jcr;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import org.junit.Test;
import org.modeshape.common.collection.Problems;
import org.modeshape.jcr.RepositoryConfiguration.AnonymousSecurity;
import org.modeshape.jcr.RepositoryConfiguration.JaasSecurity;
import org.modeshape.jcr.RepositoryConfiguration.Security;

public class RepositoryConfigurationTest {

    @Test
    public void shouldSuccessfullyValidateDefaultConfiguration() {
        assertValid(new RepositoryConfiguration("repoName"));
    }

    @Test
    public void shouldReportErrorWithNoName() {
        assertNotValid(1, "{}");
    }

    @Test
    public void shouldReportErrorWithExtraTopLevelProperties() {
        assertNotValid(1, "{ \"notValid\" : false }");
    }

    @Test
    public void shouldReportErrorWithExtraStorageProperties() {
        assertNotValid(1, "{ \"storage\" : { \"notValid\" : false } }");
    }

    @Test
    public void shouldReportErrorWithExtraWorkspacesProperties() {
        assertNotValid(1, "{ \"workspaces\" : { \"notValid\" : false } }");
    }

    @Test
    public void shouldReportErrorWithExtraSecurityProperties() {
        assertNotValid(1, "{ \"security\" : { \"notValid\" : false } }");
    }

    @Test
    public void shouldReportErrorWithExtraQueryProperties() {
        assertNotValid(1, "{ \"query\" : { \"notValid\" : false } }");
    }

    @Test
    public void shouldReportErrorWithExtraSequencingProperties() {
        assertNotValid(1, "{ \"sequencing\" : { \"notValid\" : false } }");
    }

    @Test
    public void shouldSuccessfullyValidateSampleRepositoryConfiguration() {
        assertValid("sample-repo-config.json");
    }

    @Test
    public void shouldAlwaysReturnNonNullSecurityComponent() {
        RepositoryConfiguration config = new RepositoryConfiguration("repoName");
        assertThat(config.getSecurity(), is(notNullValue()));
    }

    @Test
    public void shouldNotConfigureJaasByDefault() {
        RepositoryConfiguration config = new RepositoryConfiguration("repoName");
        Security security = config.getSecurity();
        JaasSecurity jaas = security.getJaas();
        assertThat(jaas, is(nullValue()));
    }

    @Test
    public void shouldHavePolicyByDefaultWhenConfiguringJaas() throws Exception {
        RepositoryConfiguration config = RepositoryConfiguration.read("{ \"security\" : { \"jaas\" : {} } }");
        Security security = config.getSecurity();
        JaasSecurity jaas = security.getJaas();
        assertThat(jaas, is(notNullValue()));
        assertThat(jaas.getPolicyName(), is(RepositoryConfiguration.Default.JAAS_POLICY_NAME));
    }

    @Test
    public void shouldHaveDefinedPolicyWhenConfiguringJaas() throws Exception {
        RepositoryConfiguration config = RepositoryConfiguration.read("{ \"security\" : { \"jaas\" : { \"policyName\" : \"mypolicy\" } } }");
        Security security = config.getSecurity();
        JaasSecurity jaas = security.getJaas();
        assertThat(jaas, is(notNullValue()));
        assertThat(jaas.getPolicyName(), is("mypolicy"));
    }

    @Test
    public void shouldConfigureAnonymousByDefault() {
        RepositoryConfiguration config = new RepositoryConfiguration("repoName");
        Security security = config.getSecurity();
        AnonymousSecurity anon = security.getAnonymous();
        assertThat(anon, is(notNullValue()));
        assertThat(anon.getAnonymousUsername(), is(RepositoryConfiguration.Default.ANONYMOUS_USERNAME));
        assertThat(anon.getAnonymousRoles(), is(RepositoryConfiguration.Default.ANONYMOUS_ROLES));
    }

    @Test
    public void shouldNotConfigureAnonymousIfNoRolesAreSpecified() throws Exception {
        RepositoryConfiguration config = RepositoryConfiguration.read("{ \"security\" : { \"anonymous\" : { \"roles\" : [] } } }");
        Security security = config.getSecurity();
        AnonymousSecurity anon = security.getAnonymous();
        assertThat(anon, is(nullValue()));
    }

    @Test
    public void shouldHaveDefinedRolesWhenConfiguringAnonymous() throws Exception {
        RepositoryConfiguration config = RepositoryConfiguration.read("{ \"security\" : { \"jaas\" : { \"policyName\" : \"mypolicy\" } } }");
        Security security = config.getSecurity();
        JaasSecurity jaas = security.getJaas();
        assertThat(jaas, is(notNullValue()));
        assertThat(jaas.getPolicyName(), is("mypolicy"));
    }

    @Test
    public void shouldHaveDefinedAnonymousUsernameWhenConfiguringAnonymous() throws Exception {
        RepositoryConfiguration config = RepositoryConfiguration.read("{ \"security\" : { \"jaas\" : { \"policyName\" : \"mypolicy\" } } }");
        Security security = config.getSecurity();
        JaasSecurity jaas = security.getJaas();
        assertThat(jaas, is(notNullValue()));
        assertThat(jaas.getPolicyName(), is("mypolicy"));
    }

    @Test
    public void shouldAlwaysReturnNonNullQueryComponent() {
        RepositoryConfiguration config = new RepositoryConfiguration("repoName");
        assertThat(config.getQuery(), is(notNullValue()));
    }

    @Test
    public void shouldAlwaysReturnNonNullSequencingComponent() {
        RepositoryConfiguration config = new RepositoryConfiguration("repoName");
        assertThat(config.getSequencing(), is(notNullValue()));
    }

    protected void assertValid( RepositoryConfiguration config ) {
        Problems results = config.validate();
        assertThat(results.toString(), results.hasProblems(), is(false));
    }

    protected void assertValid( String configContent ) {
        assertValid(assertRead(configContent));
    }

    protected void assertNotValid( int numberOfErrors,
                                   RepositoryConfiguration config ) {
        Problems results = config.validate();
        assertThat(results.toString(), results.hasProblems(), is(true));
        assertThat(results.toString(), results.hasErrors(), is(true));
        assertThat(results.toString(), results.errorCount(), is(numberOfErrors));
    }

    protected void assertNotValid( int numberOfErrors,
                                   String configContent ) {
        assertNotValid(numberOfErrors, assertRead(configContent));
    }

    protected RepositoryConfiguration assertRead( String content ) {
        try {
            return RepositoryConfiguration.read(content);
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
            return null;
        }
    }

}
