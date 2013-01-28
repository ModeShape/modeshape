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

package org.modeshape.test.kit;

import java.io.File;
import java.io.IOException;
import javax.annotation.Resource;
import javax.sql.DataSource;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.DependencyResolvers;
import org.jboss.shrinkwrap.resolver.api.maven.MavenDependencyResolver;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.modeshape.jcr.api.Repository;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Arquillian based test which verifies that the standard ModeShape distribution kit starts up correctly and its deployed applications
 * and subsystems are accessible.
 *
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
@RunWith( Arquillian.class )
public class JBossASKitTest {

    @Resource( mappedName = "datasources/ModeShapeDS" )
    private DataSource modeshapeDS;

    @Resource( mappedName = "/jcr/artifacts" )
    private Repository artifactsRepo;

    @Resource( mappedName = "/jcr/sample" )
    private Repository sampleRepo;


    @Deployment
    public static WebArchive createDeployment() {
        MavenDependencyResolver mavenResolver = DependencyResolvers.use(MavenDependencyResolver.class)
                                                                   .goOffline()
                                                                   .loadMetadataFromPom("pom.xml")
                                                                   .artifact("org.apache.httpcomponents:httpclient:jar")
                                                                   .scope("test");
        return ShrinkWrap.create(WebArchive.class, "as7-kit-test.war")
                         .addAsLibraries(mavenResolver.resolveAsFiles())
                         .addAsWebInfResource(EmptyAsset.INSTANCE, ArchivePaths.create("beans.xml"))
                         .setManifest(new File("src/main/webapp/META-INF/MANIFEST.MF"));
    }

    @Test
    public void repositoriesShouldBeAccessible() throws Exception {
        assertNotNull(artifactsRepo);
        assertNotNull(artifactsRepo.login("default"));
        assertNotNull(sampleRepo);
        assertNotNull(sampleRepo.login("default"));
    }

    @Test
    public void dataSourcesShouldBeAccessible() throws Exception {
        assertNotNull(modeshapeDS);
        assertNotNull(modeshapeDS.getConnection("admin", "admin"));
    }

    @Test
    public void webApplicationsShouldBeAccessible() throws Exception {
        DefaultHttpClient httpClient = new DefaultHttpClient();

        httpClient.setCredentialsProvider(new BasicCredentialsProvider() {
            @Override
            public Credentials getCredentials( AuthScope authscope ) {
                //defined via modeshape-user and modeshape-roles
                return new UsernamePasswordCredentials("admin", "admin");
            }
        });
        assertURIisAccessible("http://localhost:8080/modeshape-webdav", httpClient);
        assertURIisAccessible("http://localhost:8080/modeshape-rest", httpClient);
        assertURIisAccessible("http://localhost:8080/modeshape-cmis", httpClient);
    }

    private void assertURIisAccessible( String uri,
                                        HttpClient httpClient ) throws IOException {
        HttpGet get = new HttpGet(uri);
        HttpResponse httpResponse = httpClient.execute(get);
        assertEquals(HttpStatus.SC_OK, httpResponse.getStatusLine().getStatusCode());
        EntityUtils.consume(httpResponse.getEntity());
    }
}
