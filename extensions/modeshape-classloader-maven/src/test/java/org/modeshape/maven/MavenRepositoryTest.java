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
package org.modeshape.maven;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import javax.jcr.RepositoryException;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.common.util.IoUtil;
import org.modeshape.common.util.StringUtil;
import org.modeshape.maven.spi.JcrMavenUrlProvider;

/**
 * @author Randall Hauch
 */
public class MavenRepositoryTest extends AbstractJcrRepositoryTest {

    public static final String MAVEN_PATH = "/maven";
    public static final String PATH_TO_TEST_POM_A = "testPomA.xml";
    public static final String PATH_TO_EMPTY_JAR = "empty.jar";

    private Properties urlProviderProperties;
    private JcrMavenUrlProvider urlProvider;
    private MavenRepository maven;
    private MavenId mavenId1;
    private MavenId mavenId2;
    private ClassLoader parentLoader;

    private MavenId projectA;
    private MavenId projectB;
    private MavenId projectC;
    private MavenId projectD;
    private MavenId projectE;
    private MavenId projectF;
    private MavenId projectG;
    private MavenId projectH;
    private MavenId projectI;

    @Before
    public void beforeEach() throws Exception {
        // Configure the JCR URL provider to use the repository ...
        this.urlProviderProperties = new Properties();
        this.urlProviderProperties.setProperty(JcrMavenUrlProvider.WORKSPACE_NAME, WORKSPACE_NAME);
        this.urlProviderProperties.setProperty(JcrMavenUrlProvider.REPOSITORY_PATH, "/path/to/repository/root");
        this.urlProviderProperties.setProperty(JcrMavenUrlProvider.USERNAME, USERNAME);
        this.urlProviderProperties.setProperty(JcrMavenUrlProvider.PASSWORD, PASSWORD);
        this.urlProvider = new JcrMavenUrlProvider();
        this.urlProvider.setRepository(getRepository());
        // this.urlProvider.configure(urlProviderProperties);

        // Configure the maven repository ...
        this.maven = new MavenRepository(this.urlProvider);
        this.mavenId1 = new MavenId("org.modeshape", "modeshape-maven", "1.0-SNAPSHOT");
        this.mavenId2 = new MavenId("org.modeshape", "dna-common", "1.0.2");
        this.parentLoader = null;

        // Load the test libraries into the fake repository
        //
        // Here are the dependencies, in order:
        // ProjectI --> ProjectH, ProjectF
        // ProjectH --> ProjectG, ProjectB
        // ProjectG --> ProjectC, ProjectE (excluding ProjectB)
        // ProjectF --> ProjectE, ProjectC, ProjectD
        // ProjectE --> ProjectD
        // ProjectD --> ProjectA, ProjectB
        // ProjectC --> nothing
        // ProjectB --> nothing
        // ProjectA --> nothing
        //
        // The classpaths (excluding duplicates) are therefore:
        // ProjectI --> I, H, F, G, B, C, E, D A
        // ProjectH --> H, G, B, C, E ,D A
        // ProjectG --> G, C, E, D, A
        // ProjectF --> E, C, D, A, B
        // ProjectE --> E, D, A, B
        // ProjectD --> D, A, B
        // ProjectC --> C
        // ProjectB --> B
        // ProjectA --> A
        //   
        // loadTestLibrary("org.jboss.example:ProjectA:1.0", "test/dependency/case1/testProjectA.xml");
        // loadTestLibrary("org.jboss.example:ProjectB:1.0", "test/dependency/case1/testProjectB.xml");
        // loadTestLibrary("org.jboss.example:ProjectC:1.0", "test/dependency/case1/testProjectC.xml");
        // loadTestLibrary("org.jboss.example:ProjectD:1.0", "test/dependency/case1/testProjectD.xml");
        // loadTestLibrary("org.jboss.example:ProjectE:1.0", "test/dependency/case1/testProjectE.xml");
        // loadTestLibrary("org.jboss.example:ProjectF:1.0", "test/dependency/case1/testProjectF.xml");
        // loadTestLibrary("org.jboss.example:ProjectG:1.0", "test/dependency/case1/testProjectG.xml");
        // loadTestLibrary("org.jboss.example:ProjectH:1.0", "test/dependency/case1/testProjectH.xml");
        // loadTestLibrary("org.jboss.example:ProjectI:1.0", "test/dependency/case1/testProjectI.xml");

        this.projectA = new MavenId("org.jboss.example:ProjectA:1.0");
        this.projectB = new MavenId("org.jboss.example:ProjectB:1.0");
        this.projectC = new MavenId("org.jboss.example:ProjectC:1.0");
        this.projectD = new MavenId("org.jboss.example:ProjectD:1.0");
        this.projectE = new MavenId("org.jboss.example:ProjectE:1.0");
        this.projectF = new MavenId("org.jboss.example:ProjectF:1.0");
        this.projectG = new MavenId("org.jboss.example:ProjectG:1.0");
        this.projectH = new MavenId("org.jboss.example:ProjectH:1.0");
        this.projectI = new MavenId("org.jboss.example:ProjectI:1.0");

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void startRepository() throws RepositoryException, IOException {
        super.startRepository();
        this.urlProvider.configure(urlProviderProperties);

    }

    public void loadTestLibraries() throws Exception {
        // Load the test libraries into the fake repository
        //
        // Here are the dependencies, in order:
        // ProjectI --> ProjectH, ProjectF
        // ProjectH --> ProjectG, ProjectB
        // ProjectG --> ProjectC, ProjectE (excluding ProjectB)
        // ProjectF --> ProjectE, ProjectC, ProjectD
        // ProjectE --> ProjectD
        // ProjectD --> ProjectA, ProjectB
        // ProjectC --> nothing
        // ProjectB --> nothing
        // ProjectA --> nothing
        //
        // The classpaths (excluding duplicates) are therefore:
        // ProjectI --> I, H, F, G, B, C, E, D A
        // ProjectH --> H, G, B, C, E ,D A
        // ProjectG --> G, C, E, D, A
        // ProjectF --> E, C, D, A, B
        // ProjectE --> E, D, A, B
        // ProjectD --> D, A, B
        // ProjectC --> C
        // ProjectB --> B
        // ProjectA --> A
        //   
        loadTestLibrary("org.jboss.example:ProjectA:1.0", "test/dependency/case1/testProjectA.xml");
        loadTestLibrary("org.jboss.example:ProjectB:1.0", "test/dependency/case1/testProjectB.xml");
        loadTestLibrary("org.jboss.example:ProjectC:1.0", "test/dependency/case1/testProjectC.xml");
        loadTestLibrary("org.jboss.example:ProjectD:1.0", "test/dependency/case1/testProjectD.xml");
        loadTestLibrary("org.jboss.example:ProjectE:1.0", "test/dependency/case1/testProjectE.xml");
        loadTestLibrary("org.jboss.example:ProjectF:1.0", "test/dependency/case1/testProjectF.xml");
        loadTestLibrary("org.jboss.example:ProjectG:1.0", "test/dependency/case1/testProjectG.xml");
        loadTestLibrary("org.jboss.example:ProjectH:1.0", "test/dependency/case1/testProjectH.xml");
        loadTestLibrary("org.jboss.example:ProjectI:1.0", "test/dependency/case1/testProjectI.xml");
    }

    protected void setRepositoryContent( MavenId mavenId,
                                         ArtifactType artifactType,
                                         SignatureType signatureType,
                                         InputStream content ) throws Exception {
        // Set the content ...
        URL url = this.urlProvider.getUrl(mavenId, artifactType, signatureType, true);
        assertThat(url, is(notNullValue()));
        URLConnection connection = url.openConnection();
        OutputStream outputStream = connection.getOutputStream();
        try {
            IoUtil.write(content, outputStream);
        } finally {
            try {
                if (outputStream != null) outputStream.close();
            } finally {
                content.close();
            }

        }

    }

    protected void loadTestLibrary( String mavenId,
                                    String pathToPomFile ) throws Exception {
        MavenId id = new MavenId(mavenId);
        setRepositoryContent(id, ArtifactType.POM, null, this.getClass().getClassLoader().getResourceAsStream(pathToPomFile));
        setRepositoryContent(id, ArtifactType.JAR, null, this.getClass().getClassLoader().getResourceAsStream(PATH_TO_EMPTY_JAR));
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowZeroIdsWhenGettingClassLoader() {
        maven.getClassLoader(parentLoader, (MavenId[])null);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowNullIdsWhenGettingClassLoader() {
        maven.getClassLoader(parentLoader, mavenId1, null, mavenId2);
    }

    @Test
    public void shouldConsiderNullIdToNotExist() {
        assertThat(maven.exists((MavenId)null), is(false));
    }

    @Test
    public void shouldReturnEmptySetWhenPassedAllNullIdsForCheckingWhetherProjectsExist() {
        assertThat(maven.exists(null, (MavenId)null), is(notNullValue()));
        assertThat(maven.exists(null, (MavenId)null).size(), is(0));

        assertThat(maven.exists(null, (MavenId)null), is(notNullValue()));
        assertThat(maven.exists(null, (MavenId)null).size(), is(0));
    }

    @Test
    public void shouldReturnValidUrlForMavenIdAndArtifactTypeAndSignatureType() throws Exception {
        assertThat(maven.getUrl(mavenId1, ArtifactType.JAR, null).toString(),
                   is("jcr:/default/org/modeshape/modeshape-maven/1.0-SNAPSHOT/modeshape-maven-1.0-SNAPSHOT.jar"));
        assertThat(maven.getUrl(mavenId1, ArtifactType.JAR, SignatureType.MD5).toString(),
                   is("jcr:/default/org/modeshape/modeshape-maven/1.0-SNAPSHOT/modeshape-maven-1.0-SNAPSHOT.jar.md5"));
        assertThat(maven.getUrl(mavenId1, ArtifactType.JAR, SignatureType.PGP).toString(),
                   is("jcr:/default/org/modeshape/modeshape-maven/1.0-SNAPSHOT/modeshape-maven-1.0-SNAPSHOT.jar.asc"));
        assertThat(maven.getUrl(mavenId1, ArtifactType.JAR, SignatureType.SHA1).toString(),
                   is("jcr:/default/org/modeshape/modeshape-maven/1.0-SNAPSHOT/modeshape-maven-1.0-SNAPSHOT.jar.sha1"));

        assertThat(maven.getUrl(mavenId1, ArtifactType.SOURCE, null).toString(),
                   is("jcr:/default/org/modeshape/modeshape-maven/1.0-SNAPSHOT/modeshape-maven-1.0-SNAPSHOT-sources.jar"));
        assertThat(maven.getUrl(mavenId1, ArtifactType.SOURCE, SignatureType.MD5).toString(),
                   is("jcr:/default/org/modeshape/modeshape-maven/1.0-SNAPSHOT/modeshape-maven-1.0-SNAPSHOT-sources.jar.md5"));
        assertThat(maven.getUrl(mavenId1, ArtifactType.SOURCE, SignatureType.PGP).toString(),
                   is("jcr:/default/org/modeshape/modeshape-maven/1.0-SNAPSHOT/modeshape-maven-1.0-SNAPSHOT-sources.jar.asc"));
        assertThat(maven.getUrl(mavenId1, ArtifactType.SOURCE, SignatureType.SHA1).toString(),
                   is("jcr:/default/org/modeshape/modeshape-maven/1.0-SNAPSHOT/modeshape-maven-1.0-SNAPSHOT-sources.jar.sha1"));

        assertThat(maven.getUrl(mavenId1, ArtifactType.POM, null).toString(),
                   is("jcr:/default/org/modeshape/modeshape-maven/1.0-SNAPSHOT/modeshape-maven-1.0-SNAPSHOT.pom"));
        assertThat(maven.getUrl(mavenId1, ArtifactType.POM, SignatureType.MD5).toString(),
                   is("jcr:/default/org/modeshape/modeshape-maven/1.0-SNAPSHOT/modeshape-maven-1.0-SNAPSHOT.pom.md5"));
        assertThat(maven.getUrl(mavenId1, ArtifactType.POM, SignatureType.PGP).toString(),
                   is("jcr:/default/org/modeshape/modeshape-maven/1.0-SNAPSHOT/modeshape-maven-1.0-SNAPSHOT.pom.asc"));
        assertThat(maven.getUrl(mavenId1, ArtifactType.POM, SignatureType.SHA1).toString(),
                   is("jcr:/default/org/modeshape/modeshape-maven/1.0-SNAPSHOT/modeshape-maven-1.0-SNAPSHOT.pom.sha1"));

        assertThat(maven.getUrl(mavenId1, ArtifactType.METADATA, null).toString(),
                   is("jcr:/default/org/modeshape/modeshape-maven/maven-metadata.xml"));
        assertThat(maven.getUrl(mavenId1, ArtifactType.METADATA, SignatureType.MD5).toString(),
                   is("jcr:/default/org/modeshape/modeshape-maven/maven-metadata.xml.md5"));
        assertThat(maven.getUrl(mavenId1, ArtifactType.METADATA, SignatureType.PGP).toString(),
                   is("jcr:/default/org/modeshape/modeshape-maven/maven-metadata.xml.asc"));
        assertThat(maven.getUrl(mavenId1, ArtifactType.METADATA, SignatureType.SHA1).toString(),
                   is("jcr:/default/org/modeshape/modeshape-maven/maven-metadata.xml.sha1"));
    }

    @Test
    public void shouldReturnValidUrlForMavenIdWithNoArtifactType() throws Exception {
        assertThat(maven.getUrl(mavenId1, null, null).toString(), is("jcr:/default/org/modeshape/modeshape-maven/1.0-SNAPSHOT/"));
    }

    @Test
    public void shouldReturnUrlThatCanBeReadFromAndWrittenTo() throws Exception {
        startRepository();

        // Set the content to zero-length...
        String content = "";
        setRepositoryContent(mavenId1, ArtifactType.JAR, null, new ByteArrayInputStream(content.getBytes()));

        URL url = maven.getUrl(mavenId1, ArtifactType.JAR, null);
        InputStream stream = url.openConnection().getInputStream();
        try {
            String readContent = StringUtil.read(stream);
            assertThat(readContent, is(content));
        } finally {
            if (stream != null) stream.close();
        }

        // Set the content to be something longer, then read it via the URL ...
        content = "";
        for (int i = 0; i != 100; ++i) {
            content += "The old gray mare just ain't what she used to be. Ain't what she used to be. Ain't what she used to be. ";
        }
        setRepositoryContent(mavenId1, ArtifactType.JAR, null, new ByteArrayInputStream(content.getBytes()));
        url = maven.getUrl(mavenId1, ArtifactType.JAR, null);
        stream = url.openConnection().getInputStream();
        try {
            String readContent = StringUtil.read(stream);
            assertThat(readContent, is(content));
        } finally {
            if (stream != null) stream.close();
        }

        // Set the content via the URL ...
        String updatedContent = "Updated! " + content;
        OutputStream ostream = url.openConnection().getOutputStream();
        try {
            StringUtil.write(updatedContent, ostream);
        } finally {
            if (ostream != null) ostream.close();
        }

        url = maven.getUrl(mavenId1, ArtifactType.JAR, null);
        stream = url.openConnection().getInputStream();
        String actualContent = null;
        try {
            actualContent = StringUtil.read(stream);
        } finally {
            if (stream != null) stream.close();
        }
        assertThat(actualContent, is(updatedContent));
    }

    @Test
    public void shouldGetAllDependenciesFromInputStreamToPomFile() throws Exception {
        MavenId id = new MavenId("org.modeshape:modeshape-maven:0.1-SNAPSHOT");
        MavenDependency.Scope[] scopes = MavenDependency.Scope.values();
        InputStream stream = this.getClass().getClassLoader().getResourceAsStream(PATH_TO_TEST_POM_A);
        List<MavenDependency> dependencies = this.maven.getDependencies(id, stream, scopes);

        assertThat(dependencies, is(notNullValue()));
        assertThat(dependencies.size(), is(11));

        assertThat(dependencies.get(0).toString(), is("org.modeshape:common:0.1-SNAPSHOT:"));
        assertThat(dependencies.get(0).getScope(), is(MavenDependency.Scope.getDefault()));
        assertThat(dependencies.get(0).getExclusions().size(), is(0));

        assertThat(dependencies.get(1).toString(), is("junit:junit:4.4:"));
        assertThat(dependencies.get(1).getScope(), is(MavenDependency.Scope.TEST));
        assertThat(dependencies.get(1).getExclusions().size(), is(0));

        assertThat(dependencies.get(2).toString(), is("org.jmock:jmock:2.4.0:"));
        assertThat(dependencies.get(2).getScope(), is(MavenDependency.Scope.TEST));
        assertThat(dependencies.get(2).getExclusions().size(), is(0));

        assertThat(dependencies.get(3).toString(), is("org.jmock:jmock-junit4:2.4.0:"));
        assertThat(dependencies.get(3).getScope(), is(MavenDependency.Scope.TEST));
        assertThat(dependencies.get(3).getExclusions().size(), is(0));

        assertThat(dependencies.get(4).toString(), is("org.slf4j:slf4j-api:1.4.3:"));
        assertThat(dependencies.get(4).getScope(), is(MavenDependency.Scope.COMPILE));
        assertThat(dependencies.get(4).getExclusions().size(), is(0));

        assertThat(dependencies.get(5).toString(), is("org.slf4j:slf4j-log4j12:1.4.3:"));
        assertThat(dependencies.get(5).getScope(), is(MavenDependency.Scope.TEST));
        assertThat(dependencies.get(5).getExclusions().size(), is(0));

        assertThat(dependencies.get(6).toString(), is("log4j:log4j:1.2.14:"));
        assertThat(dependencies.get(6).getScope(), is(MavenDependency.Scope.TEST));
        assertThat(dependencies.get(6).getExclusions().size(), is(0));

        assertThat(dependencies.get(7).toString(), is("javax.jcr:jcr:1.0.1:"));
        assertThat(dependencies.get(7).getScope(), is(MavenDependency.Scope.COMPILE));
        assertThat(dependencies.get(7).getExclusions().size(), is(0));

        assertThat(dependencies.get(8).toString(), is("org.apache.jackrabbit:jackrabbit-api:1.3.3:"));
        assertThat(dependencies.get(8).getScope(), is(MavenDependency.Scope.TEST));
        assertThat(dependencies.get(8).getExclusions().size(), is(2));
        assertThat(dependencies.get(8).getExclusions().contains(new MavenId("xml-apis:xml-apis")), is(true));
        assertThat(dependencies.get(8).getExclusions().contains(new MavenId("xerces:xercesImpl")), is(true));

        assertThat(dependencies.get(9).toString(), is("org.apache.jackrabbit:jackrabbit-core:1.3.3:"));
        assertThat(dependencies.get(9).getScope(), is(MavenDependency.Scope.TEST));
        assertThat(dependencies.get(9).getExclusions().size(), is(2));
        assertThat(dependencies.get(9).getExclusions().contains(new MavenId("xml-apis:xml-apis")), is(true));
        assertThat(dependencies.get(9).getExclusions().contains(new MavenId("xerces:xercesImpl")), is(true));

        assertThat(dependencies.get(10).toString(), is("org.apache.derby:derby:10.2.1.6:"));
        assertThat(dependencies.get(10).getScope(), is(MavenDependency.Scope.TEST));
        assertThat(dependencies.get(10).getExclusions().size(), is(0));
    }

    @Test
    public void shouldGetCompileAndRuntimeDependenciesFromInputStreamToPomFile() throws Exception {
        MavenId id = new MavenId("org.modeshape:modeshape-maven:0.1-SNAPSHOT");
        MavenDependency.Scope[] scopes = {MavenDependency.Scope.COMPILE, MavenDependency.Scope.RUNTIME};
        InputStream stream = this.getClass().getClassLoader().getResourceAsStream(PATH_TO_TEST_POM_A);
        List<MavenDependency> dependencies = this.maven.getDependencies(id, stream, scopes);

        assertThat(dependencies, is(notNullValue()));
        assertThat(dependencies.size(), is(3));

        stream = this.getClass().getClassLoader().getResourceAsStream(PATH_TO_TEST_POM_A);
        dependencies = this.maven.getDependencies(id, stream);

        assertThat(dependencies, is(notNullValue()));
        assertThat(dependencies.size(), is(3));

        assertThat(dependencies.get(0).toString(), is("org.modeshape:common:0.1-SNAPSHOT:"));
        assertThat(dependencies.get(0).getScope(), is(MavenDependency.Scope.getDefault()));
        assertThat(dependencies.get(0).getExclusions().size(), is(0));

        assertThat(dependencies.get(1).toString(), is("org.slf4j:slf4j-api:1.4.3:"));
        assertThat(dependencies.get(1).getScope(), is(MavenDependency.Scope.COMPILE));
        assertThat(dependencies.get(1).getExclusions().size(), is(0));

        assertThat(dependencies.get(2).toString(), is("javax.jcr:jcr:1.0.1:"));
        assertThat(dependencies.get(2).getScope(), is(MavenDependency.Scope.COMPILE));
        assertThat(dependencies.get(2).getExclusions().size(), is(0));
    }

    @Test
    public void shouldBuildCorrectClasspathForOneProjectWithNoDependencies() throws Exception {
        startRepository();
        loadTestLibraries();
        checkClasspath(new MavenId[] {projectA}, new MavenId[] {projectA});
    }

    @Test
    public void shouldBuildCorrectClasspathForMultipleProjectsWithNoDependencies() throws Exception {
        startRepository();
        loadTestLibraries();
        checkClasspath(new MavenId[] {projectA, projectB, projectC}, new MavenId[] {projectA, projectB, projectC});
    }

    @Test
    public void shouldBuildCorrectClasspathForProjectsWithVariousDependencies() throws Exception {
        startRepository();
        loadTestLibraries();
        checkClasspath(new MavenId[] {projectA}, new MavenId[] {projectA});
        checkClasspath(new MavenId[] {projectB}, new MavenId[] {projectB});
        checkClasspath(new MavenId[] {projectC}, new MavenId[] {projectC});
        checkClasspath(new MavenId[] {projectD}, new MavenId[] {projectD, projectA, projectB});
        checkClasspath(new MavenId[] {projectE}, new MavenId[] {projectE, projectD, projectA, projectB});
        checkClasspath(new MavenId[] {projectF}, new MavenId[] {projectF, projectE, projectC, projectD, projectA, projectB});
        checkClasspath(new MavenId[] {projectG}, new MavenId[] {projectG, projectC, projectE, projectD, projectA});
        checkClasspath(new MavenId[] {projectH}, new MavenId[] {projectH, projectG, projectB, projectC, projectE, projectD,
            projectA});
        checkClasspath(new MavenId[] {projectI}, new MavenId[] {projectI, projectH, projectF, projectG, projectB, projectC,
            projectE, projectD, projectA});
    }

    @Test
    public void shouldAdjustClasspathForProjectWhoseDependenciesAreChanged() throws Exception {
        startRepository();
        loadTestLibraries();

        loadTestLibrary("org.jboss.example:ProjectG:1.0", "test/dependency/case1/testProjectG.xml");
        checkClasspath(new MavenId[] {projectG}, new MavenId[] {projectG, projectC, projectE, projectD, projectA});

        loadTestLibrary("org.jboss.example:ProjectG:1.0", "test/dependency/case1/testProjectG-withNoExclusions.xml");
        maven.notifyUpdatedPom(projectG);
        checkClasspath(new MavenId[] {projectG}, new MavenId[] {projectG, projectC, projectE, projectD, projectA, projectB});
    }

    protected void checkClasspath( MavenId[] projectIds,
                                   MavenId[] classpathProjects ) {
        List<MavenId> idsExpectedOnClasspath = new ArrayList<MavenId>();
        for (int i = 0; i != classpathProjects.length; ++i) {
            idsExpectedOnClasspath.add(classpathProjects[i]);
        }

        // Get a classloader for the input projects ...
        MavenClassLoaders.ProjectClassLoader loader = (MavenClassLoaders.ProjectClassLoader)maven.getClassLoader(null, projectIds);
        assertThat(loader, is(notNullValue()));

        // Search the classpath for a non-existant resource so that all class loaders on the classpath will be searched
        // and the 'debugSearchPath' will be complete
        List<MavenId> debugSearchPath = new ArrayList<MavenId>();
        URL result = loader.findResource("non/existant/resource/name", debugSearchPath);
        assertThat(result, is(nullValue()));

        // Check the 'debugSearchPath' includes ProjectA
        assertThat(debugSearchPath, is(idsExpectedOnClasspath));

        // And search again 1000 times ...
        for (int i = 0; i != 1000; ++i) {
            loader.findResource("non/existant/resource/name/" + i);
        }
    }
}
