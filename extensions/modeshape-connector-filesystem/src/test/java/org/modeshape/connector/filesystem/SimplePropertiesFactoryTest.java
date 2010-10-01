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
package org.modeshape.connector.filesystem;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.common.util.FileUtil;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.Location;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.Property;

/**
 * 
 */
public class SimplePropertiesFactoryTest {

    private static final String JAVA_TYPE = "text/x-java-source";

    private StoreProperties factory;
    private File testArea;
    private ExecutionContext context;
    private Collection<Property> properties;
    private Location location;
    private String source;
    private Map<Name, Property> newProperties;

    @Before
    public void beforeEach() throws IOException {
        testArea = new File("target/test/properties");
        FileUtil.delete(testArea);
        testArea.mkdirs();
        FileUtil.copy(new File("src/main/java/org"), new File(testArea, "org"));
        factory = new StoreProperties();
        newProperties = new HashMap<Name, Property>();
        context = new ExecutionContext();
        context.getNamespaceRegistry().register("test", "http://www.modeshape.org/test/1.0");
    }

    @After
    public void afterEach() {
        // FileUtil.delete(testArea);
        testArea = null;
    }

    @Test
    public void shouldHaveCopiedFilesIntoTestArea() throws Exception {
        assertFolder("org");
        assertFolder("org/modeshape");
        assertFolder("org/modeshape/connector");
        assertFolder("org/modeshape/connector/filesystem");
        assertFile("org/modeshape/connector/filesystem/CustomPropertiesFactory.java");
        assertFile("org/modeshape/connector/filesystem/FileSystemI18n.java");
        assertFile("org/modeshape/connector/filesystem/FileSystemRepository.java");
        assertFile("org/modeshape/connector/filesystem/FileSystemSource.java");
        assertFile("org/modeshape/connector/filesystem/FileSystemWorkspace.java");
        assertFile("org/modeshape/connector/filesystem/package-info.java");
        assertFile("org/modeshape/connector/filesystem/StoreProperties.java");
    }

    @Test
    public void shouldReadPropertiesForDirectoryWhenExtraPropertiesFileDoesNotExist() throws Exception {
        properties = factory.getDirectoryProperties(context, location, fileAt("org"));
        assertThat(properties.isEmpty(), is(true));
        properties = factory.getDirectoryProperties(context, location, fileAt("org/modeshape"));
        assertThat(properties.isEmpty(), is(true));
        properties = factory.getDirectoryProperties(context, location, fileAt("org/modeshape/connector"));
        assertThat(properties.isEmpty(), is(true));
        properties = factory.getDirectoryProperties(context, location, fileAt("org/modeshape/connector/filesystem"));
        assertThat(properties.isEmpty(), is(true));
    }

    @Test
    public void shouldReadPropertiesForFileWhenExtraPropertiesFileDoesNotExist() throws Exception {
        File file = fileAt("org/modeshape/connector/filesystem/CustomPropertiesFactory.java");
        properties = factory.getFileProperties(context, location, file);
        assertThat(properties.isEmpty(), is(true));
    }

    @Test
    public void shouldReadPropertiesForResourceWhenExtraPropertiesFileDoesNotExist() throws Exception {
        File file = fileAt("org/modeshape/connector/filesystem/CustomPropertiesFactory.java");
        properties = factory.getResourceProperties(context, location, file, JAVA_TYPE);
        assertThat(properties.isEmpty(), is(true));
    }

    @Test
    public void shouldWritePropertiesForDirectory() throws Exception {
        File dir = fileAt("org");
        addProperties();
        factory.recordDirectoryProperties(context, source, location, dir, newProperties);
        properties = factory.getDirectoryProperties(context, location, dir);
        assertPropertiesMatch();
    }

    protected File fileAt( String path ) {
        return new File(testArea, path);
    }

    protected void addProperty( String name,
                                Object... values ) {
        Name propName = context.getValueFactories().getNameFactory().create(name);
        Property property = context.getPropertyFactory().create(propName, values);
        newProperties.put(property.getName(), property);
    }

    protected void addProperties() {
        addProperty("test:stringProp", "val1");
        addProperty("test:stringPropWithOddChars", "val1 has spaces and \"quotes\" and \n new line characters");
        addProperty("test:longProp", 2L);
        addProperty("test:doubleProp", 3.523);
        addProperty("test:booleanProp", true);
        addProperty("test:binaryProp", context.getValueFactories().getBinaryFactory().create("This is the content".getBytes()));
    }

    protected void assertPropertiesMatch() {
        assertThat(properties.size(), is(newProperties.size()));
        for (Property prop : properties) {
            assertThat(newProperties.containsKey(prop.getName()), is(true));
            assertThat(newProperties.get(prop.getName()), is(prop));
        }
    }

    protected void assertFolder( String path ) {
        File file = new File(testArea, path);
        assertThat(file.exists(), is(true));
        assertThat(file.canRead(), is(true));
        assertThat(file.isDirectory(), is(true));
    }

    protected void assertFile( String path ) {
        File file = new File(testArea, path);
        assertThat(file.exists(), is(true));
        assertThat(file.canRead(), is(true));
        assertThat(file.isFile(), is(true));
        assertThat(file.length() > 0L, is(true));
    }
}
