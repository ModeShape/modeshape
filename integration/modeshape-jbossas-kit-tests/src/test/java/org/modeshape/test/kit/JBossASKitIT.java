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
package org.modeshape.test.kit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import java.io.File;
import java.io.IOException;
import javax.annotation.Resource;
import javax.jcr.Node;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;
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
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.modeshape.jcr.api.Repository;

/**
 * Arquillian based test which verifies that the standard ModeShape distribution kit starts up correctly and its deployed applications
 * and subsystems are accessible.
 *
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
@SuppressWarnings( "deprecation" )
@RunWith( Arquillian.class )
public class JBossASKitIT {

    @Resource( mappedName = "java:/datasources/ModeShapeDS" )
    private DataSource modeshapeDS;

    @Resource( mappedName = "java:/jcr/artifacts" )
    private Repository artifactsRepo;

    @Resource( mappedName = "java:/jcr/sample" )
    private Repository sampleRepo;

    @Deployment
    public static WebArchive createDeployment() {
        File[] testDeps = Maven.configureResolver()
                               .workOffline() 
                               .loadPomFromFile("pom.xml")
                               .resolve("org.apache.httpcomponents:httpclient", "org.apache.httpcomponents:httpcore")
                               .withTransitivity()
                               .asFile();
        return ShrinkWrap.create(WebArchive.class, "as7-kit-test.war")
                         .addAsLibraries(testDeps)
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
    public void artifactsRepositoryShouldBePublishArea() throws Exception {
        Session session = artifactsRepo.login();
        Node filesFolder = session.getNode("/files");
        Assert.assertNotNull(filesFolder);
        Assert.assertEquals("nt:folder", filesFolder.getPrimaryNodeType().getName());
        NodeType[] mixins = filesFolder.getMixinNodeTypes();
        Assert.assertEquals(1, mixins.length);
        Assert.assertEquals("mode:publishArea", mixins[0].getName());
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
        assertURIisAccessible("http://localhost:8080/modeshape-explorer", httpClient);
    }

    private void assertURIisAccessible( String uri,
                                        HttpClient httpClient ) throws IOException {
        HttpGet get = new HttpGet(uri);
        HttpResponse httpResponse = httpClient.execute(get);
        assertEquals(HttpStatus.SC_OK, httpResponse.getStatusLine().getStatusCode());
        EntityUtils.consume(httpResponse.getEntity());
    }
}
