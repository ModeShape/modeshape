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
package org.modeshape.maven.spi;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Properties;
import javax.jcr.SimpleCredentials;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.common.util.StringUtil;
import org.modeshape.maven.AbstractJcrRepositoryTest;
import org.modeshape.maven.ArtifactType;
import org.modeshape.maven.MavenId;
import org.modeshape.maven.SignatureType;

/**
 * @author Randall Hauch
 */
public class JcrMavenUrlProviderTest extends AbstractJcrRepositoryTest {

    private JcrMavenUrlProvider provider;
    private Properties validProperties;
    private MavenId mavenId1;

    @Before
    public void beforeEach() throws Exception {
        File tmpFolder = new File("target/testdata/tmp");
        tmpFolder.mkdirs();
        System.setProperty("java.io.tmpdir", tmpFolder.getAbsolutePath());

        validProperties = new Properties();
        validProperties.setProperty("unused.property", "whatever");
        validProperties.setProperty(JcrMavenUrlProvider.WORKSPACE_NAME, WORKSPACE_NAME);
        validProperties.setProperty(JcrMavenUrlProvider.REPOSITORY_PATH, "/path/to/repository/root");
        validProperties.setProperty(JcrMavenUrlProvider.USERNAME, USERNAME);
        validProperties.setProperty(JcrMavenUrlProvider.PASSWORD, PASSWORD);
        mavenId1 = new MavenId("org.modeshape", "modeshape-maven", "1.0-SNAPSHOT");

        // Configure the JCR URL provider to use the repository ...
        provider = new JcrMavenUrlProvider();
        provider.setRepository(getRepository());
    }

    @Test
    public void shouldHaveNullWorkspaceNameUponDefaultConstruction() {
        assertThat(provider.getWorkspaceName(), is(nullValue()));
    }

    @Test
    public void shouldHaveNullCredentialsUponDefaultConstruction() {
        assertThat(provider.getCredentials(), is(nullValue()));
    }

    @Test
    public void shouldHaveDefaultRepositoryPathUponDefaultConstruction() {
        assertThat(provider.getPathToTopOfRepository(), is(JcrMavenUrlProvider.DEFAULT_PATH_TO_TOP_OF_MAVEN_REPOSITORY));
    }

    @Test
    public void shouldHaveNonNullPropertiesUponDefaultConstruction() {
        assertThat(provider.getProperties(), is(notNullValue()));
    }

    @Test
    public void shouldHaveNonNullPropertiesAfterConfigurationUsingNullProperties() {
        provider.configure(null);
        assertThat(provider.getProperties(), is(notNullValue()));
    }

    @Test
    public void shouldHaveCorrectPropertiesAfterConfiguration() {
        provider.configure(validProperties);
        assertThat(provider.getProperties(), is(validProperties));
        assertThat(provider.getPathToTopOfRepository(), is("/path/to/repository/root"));
        assertThat(provider.getCredentials(), is(notNullValue()));
        assertThat(provider.getCredentials() instanceof SimpleCredentials, is(true));
        SimpleCredentials simpleCreds = (SimpleCredentials)provider.getCredentials();
        assertThat(simpleCreds.getUserID(), is(USERNAME));
        assertThat(simpleCreds.getPassword(), is(PASSWORD.toCharArray()));
    }

    @Test
    public void shouldNotOverwriteNonDefaultValuesWhenNotGivenInConfigurationProperties() {
        provider.configure(validProperties);
        provider.configure(new Properties());
        assertThat(provider.getProperties(), is(new Properties()));
        // Values should not be defaults ...
        assertThat(provider.getPathToTopOfRepository(), is("/path/to/repository/root"));
        assertThat(provider.getCredentials(), is(notNullValue()));
        assertThat(provider.getCredentials() instanceof SimpleCredentials, is(true));
        SimpleCredentials simpleCreds = (SimpleCredentials)provider.getCredentials();
        assertThat(simpleCreds.getUserID(), is(USERNAME));
        assertThat(simpleCreds.getPassword(), is(PASSWORD.toCharArray()));
    }

    @Test
    public void shouldDetermineProperPathGivenMavenIdAndArtifactType() {
        assertThat(provider.getUrlPath(mavenId1, ArtifactType.JAR, null),
                   is("/org/modeshape/modeshape-maven/1.0-SNAPSHOT/modeshape-maven-1.0-SNAPSHOT.jar"));
        assertThat(provider.getUrlPath(mavenId1, ArtifactType.JAR, SignatureType.MD5),
                   is("/org/modeshape/modeshape-maven/1.0-SNAPSHOT/modeshape-maven-1.0-SNAPSHOT.jar.md5"));
        assertThat(provider.getUrlPath(mavenId1, ArtifactType.JAR, SignatureType.PGP),
                   is("/org/modeshape/modeshape-maven/1.0-SNAPSHOT/modeshape-maven-1.0-SNAPSHOT.jar.asc"));
        assertThat(provider.getUrlPath(mavenId1, ArtifactType.JAR, SignatureType.SHA1),
                   is("/org/modeshape/modeshape-maven/1.0-SNAPSHOT/modeshape-maven-1.0-SNAPSHOT.jar.sha1"));

        assertThat(provider.getUrlPath(mavenId1, ArtifactType.SOURCE, null),
                   is("/org/modeshape/modeshape-maven/1.0-SNAPSHOT/modeshape-maven-1.0-SNAPSHOT-sources.jar"));
        assertThat(provider.getUrlPath(mavenId1, ArtifactType.SOURCE, SignatureType.MD5),
                   is("/org/modeshape/modeshape-maven/1.0-SNAPSHOT/modeshape-maven-1.0-SNAPSHOT-sources.jar.md5"));
        assertThat(provider.getUrlPath(mavenId1, ArtifactType.SOURCE, SignatureType.PGP),
                   is("/org/modeshape/modeshape-maven/1.0-SNAPSHOT/modeshape-maven-1.0-SNAPSHOT-sources.jar.asc"));
        assertThat(provider.getUrlPath(mavenId1, ArtifactType.SOURCE, SignatureType.SHA1),
                   is("/org/modeshape/modeshape-maven/1.0-SNAPSHOT/modeshape-maven-1.0-SNAPSHOT-sources.jar.sha1"));

        assertThat(provider.getUrlPath(mavenId1, ArtifactType.POM, null),
                   is("/org/modeshape/modeshape-maven/1.0-SNAPSHOT/modeshape-maven-1.0-SNAPSHOT.pom"));
        assertThat(provider.getUrlPath(mavenId1, ArtifactType.POM, SignatureType.MD5),
                   is("/org/modeshape/modeshape-maven/1.0-SNAPSHOT/modeshape-maven-1.0-SNAPSHOT.pom.md5"));
        assertThat(provider.getUrlPath(mavenId1, ArtifactType.POM, SignatureType.PGP),
                   is("/org/modeshape/modeshape-maven/1.0-SNAPSHOT/modeshape-maven-1.0-SNAPSHOT.pom.asc"));
        assertThat(provider.getUrlPath(mavenId1, ArtifactType.POM, SignatureType.SHA1),
                   is("/org/modeshape/modeshape-maven/1.0-SNAPSHOT/modeshape-maven-1.0-SNAPSHOT.pom.sha1"));

        assertThat(provider.getUrlPath(mavenId1, ArtifactType.METADATA, null),
                   is("/org/modeshape/modeshape-maven/maven-metadata.xml"));
        assertThat(provider.getUrlPath(mavenId1, ArtifactType.METADATA, SignatureType.MD5),
                   is("/org/modeshape/modeshape-maven/maven-metadata.xml.md5"));
        assertThat(provider.getUrlPath(mavenId1, ArtifactType.METADATA, SignatureType.PGP),
                   is("/org/modeshape/modeshape-maven/maven-metadata.xml.asc"));
        assertThat(provider.getUrlPath(mavenId1, ArtifactType.METADATA, SignatureType.SHA1),
                   is("/org/modeshape/modeshape-maven/maven-metadata.xml.sha1"));
    }

    @Test
    public void shouldDeterminePropertPathForMavenIdWithNoArtifactType() {
        assertThat(provider.getUrlPath(mavenId1, null, null), is("/org/modeshape/modeshape-maven/1.0-SNAPSHOT/"));
    }

    @Test
    public void shouldReturnValidUrlForMavenIdAndArtifactTypeAndSignatureType() throws Exception {
        assertThat(provider.getUrl(mavenId1, ArtifactType.JAR, null, false).toString(),
                   is("jcr://org/modeshape/modeshape-maven/1.0-SNAPSHOT/modeshape-maven-1.0-SNAPSHOT.jar"));
        assertThat(provider.getUrl(mavenId1, ArtifactType.JAR, SignatureType.MD5, false).toString(),
                   is("jcr://org/modeshape/modeshape-maven/1.0-SNAPSHOT/modeshape-maven-1.0-SNAPSHOT.jar.md5"));
        assertThat(provider.getUrl(mavenId1, ArtifactType.JAR, SignatureType.PGP, false).toString(),
                   is("jcr://org/modeshape/modeshape-maven/1.0-SNAPSHOT/modeshape-maven-1.0-SNAPSHOT.jar.asc"));
        assertThat(provider.getUrl(mavenId1, ArtifactType.JAR, SignatureType.SHA1, false).toString(),
                   is("jcr://org/modeshape/modeshape-maven/1.0-SNAPSHOT/modeshape-maven-1.0-SNAPSHOT.jar.sha1"));

        assertThat(provider.getUrl(mavenId1, ArtifactType.SOURCE, null, false).toString(),
                   is("jcr://org/modeshape/modeshape-maven/1.0-SNAPSHOT/modeshape-maven-1.0-SNAPSHOT-sources.jar"));
        assertThat(provider.getUrl(mavenId1, ArtifactType.SOURCE, SignatureType.MD5, false).toString(),
                   is("jcr://org/modeshape/modeshape-maven/1.0-SNAPSHOT/modeshape-maven-1.0-SNAPSHOT-sources.jar.md5"));
        assertThat(provider.getUrl(mavenId1, ArtifactType.SOURCE, SignatureType.PGP, false).toString(),
                   is("jcr://org/modeshape/modeshape-maven/1.0-SNAPSHOT/modeshape-maven-1.0-SNAPSHOT-sources.jar.asc"));
        assertThat(provider.getUrl(mavenId1, ArtifactType.SOURCE, SignatureType.SHA1, false).toString(),
                   is("jcr://org/modeshape/modeshape-maven/1.0-SNAPSHOT/modeshape-maven-1.0-SNAPSHOT-sources.jar.sha1"));

        assertThat(provider.getUrl(mavenId1, ArtifactType.POM, null, false).toString(),
                   is("jcr://org/modeshape/modeshape-maven/1.0-SNAPSHOT/modeshape-maven-1.0-SNAPSHOT.pom"));
        assertThat(provider.getUrl(mavenId1, ArtifactType.POM, SignatureType.MD5, false).toString(),
                   is("jcr://org/modeshape/modeshape-maven/1.0-SNAPSHOT/modeshape-maven-1.0-SNAPSHOT.pom.md5"));
        assertThat(provider.getUrl(mavenId1, ArtifactType.POM, SignatureType.PGP, false).toString(),
                   is("jcr://org/modeshape/modeshape-maven/1.0-SNAPSHOT/modeshape-maven-1.0-SNAPSHOT.pom.asc"));
        assertThat(provider.getUrl(mavenId1, ArtifactType.POM, SignatureType.SHA1, false).toString(),
                   is("jcr://org/modeshape/modeshape-maven/1.0-SNAPSHOT/modeshape-maven-1.0-SNAPSHOT.pom.sha1"));

        assertThat(provider.getUrl(mavenId1, ArtifactType.METADATA, null, false).toString(),
                   is("jcr://org/modeshape/modeshape-maven/maven-metadata.xml"));
        assertThat(provider.getUrl(mavenId1, ArtifactType.METADATA, SignatureType.MD5, false).toString(),
                   is("jcr://org/modeshape/modeshape-maven/maven-metadata.xml.md5"));
        assertThat(provider.getUrl(mavenId1, ArtifactType.METADATA, SignatureType.PGP, false).toString(),
                   is("jcr://org/modeshape/modeshape-maven/maven-metadata.xml.asc"));
        assertThat(provider.getUrl(mavenId1, ArtifactType.METADATA, SignatureType.SHA1, false).toString(),
                   is("jcr://org/modeshape/modeshape-maven/maven-metadata.xml.sha1"));
    }

    @Test
    public void shouldReturnValidUrlForMavenIdWithNoArtifactType() throws Exception {
        assertThat(provider.getUrl(mavenId1, null, null, false).toString(),
                   is("jcr://org/modeshape/modeshape-maven/1.0-SNAPSHOT/"));
    }

    @Test
    public void shouldReturnUrlThatCanBeReadFromAndWrittenTo() throws Exception {
        startRepository();
        provider.configure(validProperties);

        // Check for existing content ...
        String content = "";
        URL url = provider.getUrl(mavenId1, ArtifactType.JAR, null, false);
        // assertThat(url, is(nullValue()));

        // Set the content ...
        url = provider.getUrl(mavenId1, ArtifactType.JAR, null, true);
        assertThat(url, is(notNullValue()));
        URLConnection connection = url.openConnection();
        OutputStream outputStream = connection.getOutputStream();
        try {
            StringUtil.write(content, outputStream);
        } finally {
            if (outputStream != null) outputStream.close();
        }

        // Read the content ...
        url = provider.getUrl(mavenId1, ArtifactType.JAR, null, false);
        assertThat(url, is(notNullValue()));
        connection = url.openConnection();
        InputStream stream = connection.getInputStream();
        try {
            String readContent = StringUtil.read(stream);
            assertThat(readContent, is(content));
        } finally {
            if (stream != null) stream.close();
        }

        // Change the content to be something longer, then read it via the URL ...
        content = "";
        for (int i = 0; i != 100; ++i) {
            content += "The old gray mare just ain't what she used to be. Ain't what she used to be. Ain't what she used to be. ";
        }

        // Set the content ...
        url = provider.getUrl(mavenId1, ArtifactType.JAR, null, true);
        assertThat(url, is(notNullValue()));
        connection = url.openConnection();
        outputStream = connection.getOutputStream();
        try {
            StringUtil.write(content, outputStream);
        } finally {
            if (outputStream != null) outputStream.close();
        }

        // Read the content ...
        url = provider.getUrl(mavenId1, ArtifactType.JAR, null, false);
        assertThat(url, is(notNullValue()));
        connection = url.openConnection();
        stream = connection.getInputStream();
        try {
            String readContent = StringUtil.read(stream);
            assertThat(readContent, is(content));
        } finally {
            if (stream != null) stream.close();
        }

    }

}
