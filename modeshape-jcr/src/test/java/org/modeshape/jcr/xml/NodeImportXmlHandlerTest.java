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

package org.modeshape.jcr.xml;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import java.io.InputStream;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.common.FixFor;
import org.modeshape.common.text.NoOpEncoder;
import org.modeshape.jcr.ExecutionContext;
import org.modeshape.jcr.JcrLexicon;
import org.modeshape.jcr.api.JcrConstants;
import org.modeshape.jcr.value.NamespaceRegistry;
import org.modeshape.jcr.value.Path;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 * Test case for {@link NodeImportXmlHandler}
 * 
 * @author Randall Hauch
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public class NodeImportXmlHandlerTest {
    private static final String NT_NAMESPACE_URI = "http://www.jcp.org/jcr/nt/1.0";

    private NodeImportXmlHandler handler;
    protected ExecutionContext context;
    protected Map<Path, NodeImportXmlHandler.ImportElement> parseResults;
    private NodeImportDestination parseDestination;

    @Before
    public void beforeEach() {
        context = new ExecutionContext();
        context.getNamespaceRegistry().register(JcrLexicon.Namespace.PREFIX, JcrLexicon.Namespace.URI);
        context.getNamespaceRegistry().register("nt", NT_NAMESPACE_URI);
        parseDestination = new NodeImportDestination() {
            @Override
            public ExecutionContext getExecutionContext() {
                return context;
            }

            @Override
            public void submit( LinkedHashMap<Path, NodeImportXmlHandler.ImportElement> parseResults ) {
                NodeImportXmlHandlerTest.this.parseResults = parseResults;
            }
        };
        handler = new NodeImportXmlHandler(parseDestination);
    }

    @Test( expected = SAXException.class )
    public void shouldNotParseXmlWithInvalidRootElement() throws Exception {
        parse("xmlImport/docWithoutJcrRoot.xml");
    }

    @Test
    public void shouldParseXmlDocumentWithoutNamespaces() throws Exception {
        parse("xmlImport/docWithoutNamespaces.xml");

        assertNode("Cars");
        assertNode("Cars/Hybrid");
        assertNode("Cars/Hybrid/car", "name=Toyota Prius", "maker=Toyota", "model=Prius");
        assertNode("Cars/Hybrid/car[2]", "name=Toyota Highlander", "maker=Toyota", "model=Highlander");
        assertNode("Cars/Hybrid/car[3]", "name=Nissan Altima", "maker=Nissan", "model=Altima");
        assertNode("Cars/Sports");
        assertNode("Cars/Sports/car", "name=Aston Martin DB9", "maker=Aston Martin", "model=DB9");
        assertNode("Cars/Sports/car[2]", "name=Infiniti G37", "maker=Infiniti", "model=G37");
    }

    @Test
    public void shouldParseXmlDocumentWithNamespaces() throws Exception {
        context.getNamespaceRegistry().register("c", "http://default.namespace.com");
        parse("xmlImport/docWithNamespaces.xml");
        // Check the generated content.
        // Note that the attribute name DOES match, so the nodes names come from "jcr:name" attribute
        // Also, the "jcr:name" attribute values use the default namespace, which is "c" in the registry
        assertNode("c:Cars");
        assertNode("c:Cars/c:Hybrid");
        assertNode("c:Cars/c:Hybrid/c:Toyota Prius", "c:maker=Toyota", "c:model=Prius");
        assertNode("c:Cars/c:Hybrid/c:Toyota Highlander", "c:maker=Toyota", "c:model=Highlander");
        assertNode("c:Cars/c:Hybrid/c:Nissan Altima", "c:maker=Nissan", "c:model=Altima");
        assertNode("c:Cars/c:Sports");
        assertNode("c:Cars/c:Sports/c:Aston Martin DB9", "c:maker=Aston Martin", "c:model=DB9");
        assertNode("c:Cars/c:Sports/c:Infiniti G37", "c:maker=Infiniti", "c:model=G37");
    }

    @Test
    public void shouldParseXmlDocumentWithNestedNamespaceDeclarations() throws Exception {
        context.getNamespaceRegistry().register("c", "http://default.namespace.com");
        context.getNamespaceRegistry().register("i", "http://attributes.com");
        parse("xmlImport/docWithNestedNamespaces.xml");
        // Check the generated content; note that the attribute name DOES match, so the nodes names come from "jcr:name" attribute
        assertNode("Cars");
        assertNode("Cars/c:Hybrid");
        assertNode("Cars/c:Hybrid/c:Toyota Prius", "c:maker=Toyota", "c:model=Prius");
        assertNode("Cars/c:Hybrid/c:Toyota Highlander", "c:maker=Toyota", "c:model=Highlander");
        assertNode("Cars/c:Hybrid/c:Nissan Altima", "c:maker=Nissan", "c:model=Altima");
        assertNode("Cars/Sports");
        assertNode("Cars/Sports/Aston Martin DB9", "i:maker=Aston Martin", "model=DB9");
        assertNode("Cars/Sports/Infiniti G37", "i:maker=Infiniti", "model=G37");
    }

    @Test
    public void shouldParseXmlDocumentWithNamespacesThatAreNotYetInRegistry() throws Exception {
        NamespaceRegistry reg = context.getNamespaceRegistry();
        reg.unregister(JcrLexicon.Namespace.URI);
        reg.unregister(NT_NAMESPACE_URI);
        // Verify the prefixes don't exist ...
        assertThat(reg.getPrefixForNamespaceUri(JcrLexicon.Namespace.URI, false), is(nullValue()));
        assertThat(reg.getPrefixForNamespaceUri(NT_NAMESPACE_URI, false), is(nullValue()));
        assertThat(reg.getPrefixForNamespaceUri("http://default.namespace.com", false), is(nullValue()));
        // Parse the XML file ...
        parse("xmlImport/docWithNestedNamespaces.xml");
        // Get the prefix for the default namespace ...
        String c = reg.getPrefixForNamespaceUri("http://default.namespace.com", false);
        String i = reg.getPrefixForNamespaceUri("http://attributes.com", false);
        String d = reg.getPrefixForNamespaceUri(reg.getDefaultNamespaceUri(), false);
        assertThat("Namespace not properly registered in primary registry", c, is(notNullValue()));
        assertThat("Namespace not properly registered in primary registry", d, is(notNullValue()));
        assertThat("Namespace not properly registered in primary registry", i, is(notNullValue()));
        if (c.length() != 0) {
            c = c + ":";
        }
        if (d.length() != 0) {
            d = d + ":";
        }
        if (i.length() != 0) {
            i = i + ":";
        }
        // Check the generated content; note that the attribute name DOES match, so the nodes names come from "jcr:name" attribute
        assertNode(d + "Cars");
        assertNode(d + "Cars/" + c + "Hybrid");
        assertNode(d + "Cars/" + c + "Hybrid/" + c + "Toyota Prius", c + "maker=Toyota", c + "model=Prius");
        assertNode(d + "Cars/" + c + "Hybrid/" + c + "Toyota Highlander", c + "maker=Toyota", c + "model=Highlander");
        assertNode(d + "Cars/" + c + "Hybrid/" + c + "Nissan Altima", c + "maker=Nissan", c + "model=Altima");
        assertNode(d + "Cars/" + d + "Sports");
        assertNode(d + "Cars/" + d + "Sports/Aston Martin DB9", i + "maker=Aston Martin", "model=DB9");
        assertNode(d + "Cars/" + d + "Sports/Infiniti G37", i + "maker=Infiniti", "model=G37");
    }

    @Test
    public void shouldParseXmlDocumentWithoutDefaultNamespaceThatUsesNameAttribute() throws Exception {
        parse("xmlImport/docWithNamespacesWithoutDefault.xml");
        // Check the generated content; note that the attribute name DOES match, so the nodes names come from "jcr:name" attribute
        assertNode("Cars");
        assertNode("Cars/Hybrid");
        assertNode("Cars/Hybrid/Toyota Prius", "maker=Toyota", "model=Prius");
        assertNode("Cars/Hybrid/Toyota Highlander", "maker=Toyota", "model=Highlander");
        assertNode("Cars/Hybrid/Nissan Altima", "maker=Nissan", "model=Altima");
        assertNode("Cars/Sports");
        assertNode("Cars/Sports/Aston Martin DB9", "maker=Aston Martin", "model=DB9");
        assertNode("Cars/Sports/Infiniti G37", "maker=Infiniti", "model=G37");
    }

    @Test
    public void shouldParseXmlDocumentThatContainsNoContent() throws Exception {
        parse("xmlImport/docWithOnlyRootElement.xml");
        assertTrue(parseResults.isEmpty());
    }

    @Test
    public void shouldParseXmlDocumentWithXmlComments() throws Exception {
        context.getNamespaceRegistry().register("c", "http://default.namespace.com");
        parse("xmlImport/docWithComments.xml");
        assertNode("c:Cars");
        assertNode("c:Cars/c:Hybrid");
        assertNode("c:Cars/c:Hybrid/c:Toyota Prius", "c:maker=Toyota", "c:model=Prius");
        assertNode("c:Cars/c:Hybrid/c:Toyota Highlander", "c:maker=Toyota", "c:model=Highlander");
        assertNode("c:Cars/c:Hybrid/c:Nissan Altima", "c:maker=Nissan", "c:model=Altima");
        assertNode("c:Cars/c:Sports");
        assertNode("c:Cars/c:Sports/c:Aston Martin DB9", "c:maker=Aston Martin", "c:model=DB9");
        assertNode("c:Cars/c:Sports/c:Infiniti G37", "c:maker=Infiniti", "c:model=G37");
    }

    @Test
    public void shouldParseXmlDocumentWithoutNamespacesUsingTypeAttributeValue() throws Exception {
        String typeAttribute = JcrConstants.JCR_PRIMARY_TYPE;
        String typeAttributeValue = JcrConstants.NT_UNSTRUCTURED;
        handler = new NodeImportXmlHandler(parseDestination, null, typeAttribute, typeAttributeValue, null, null);
        parse("xmlImport/docWithoutNamespaces.xml");
        // Check the generated content; note that the attribute name doesn't match, so the nodes don't get special names
        String unstructPrimaryType = "jcr:primaryType=nt:unstructured";
        assertNode("Cars", unstructPrimaryType);
        assertNode("Cars/Hybrid", unstructPrimaryType);
        assertNode("Cars/Hybrid/car", unstructPrimaryType, "name=Toyota Prius", "maker=Toyota", "model=Prius");
        assertNode("Cars/Hybrid/car[2]", unstructPrimaryType, "name=Toyota Highlander", "maker=Toyota", "model=Highlander");
        assertNode("Cars/Hybrid/car[3]", unstructPrimaryType, "name=Nissan Altima", "maker=Nissan", "model=Altima");
        assertNode("Cars/Sports", unstructPrimaryType);
        assertNode("Cars/Sports/car", unstructPrimaryType, "name=Aston Martin DB9", "maker=Aston Martin", "model=DB9");
        assertNode("Cars/Sports/car[2]", unstructPrimaryType, "name=Infiniti G37", "maker=Infiniti", "model=G37");
    }

    @Test
    public void shouldParseXmlDocumentWithNamespacesUsingTypeAttributeValue() throws Exception {
        context.getNamespaceRegistry().register("c", "http://default.namespace.com");
        String nameAttribute = JcrConstants.JCR_NAME;
        String typeAttribute = JcrConstants.JCR_PRIMARY_TYPE;
        String typeAttributeValue = JcrConstants.NT_UNSTRUCTURED;
        handler = new NodeImportXmlHandler(parseDestination, nameAttribute, typeAttribute, typeAttributeValue, null, null);
        parse("xmlImport/docWithNamespaces.xml");
        // Check the generated content; note that the attribute name DOES match
        String unstructPrimaryType = "jcr:primaryType=nt:unstructured";
        assertNode("c:Cars", unstructPrimaryType);
        assertNode("c:Cars/c:Hybrid", unstructPrimaryType);
        assertNode("c:Cars/c:Hybrid/c:Toyota Prius", unstructPrimaryType, "c:maker=Toyota", "c:model=Prius");
        assertNode("c:Cars/c:Hybrid/c:Toyota Highlander", unstructPrimaryType, "c:maker=Toyota", "c:model=Highlander");
        assertNode("c:Cars/c:Hybrid/c:Nissan Altima", unstructPrimaryType, "c:maker=Nissan", "c:model=Altima");
        assertNode("c:Cars/c:Sports", unstructPrimaryType);
        assertNode("c:Cars/c:Sports/c:Aston Martin DB9", unstructPrimaryType, "c:maker=Aston Martin", "c:model=DB9");
        assertNode("c:Cars/c:Sports/c:Infiniti G37", unstructPrimaryType, "c:maker=Infiniti", "c:model=G37");
    }

    @Test
    public void shouldParseXmlDocumentWithNestedProperties() throws Exception {
        context.getNamespaceRegistry().register("jcr", "http://www.jcp.org/jcr/1.0");
        handler = new NodeImportXmlHandler(parseDestination, JcrConstants.JCR_NAME, null, null, null, null);
        parse("xmlImport/docWithNestedProperties.xml");
        // Check the generated content; note that the attribute name DOES match, so the nodes names come from "jcr:name" attribute
        assertNode("Cars");
        assertNode("Cars/Hybrid");
        assertNode("Cars/Hybrid/car", "name=Toyota Prius", "maker=Toyota", "model=Prius");
        assertNode("Cars/Hybrid/car[2]", "name=Toyota Highlander", "maker=Toyota", "model=Highlander");
        assertNode("Cars/Hybrid/car[3]", "name=Nissan Altima", "maker=Nissan", "model=Altima");
        assertNode("Cars/Sports");
        assertNode("Cars/Sports/car", "name=Aston Martin DB9", "maker=Aston Martin", "model=DB9");
        assertNode("Cars/Sports/car[2]", "name=Infiniti G37", "maker=Infiniti", "category=Turbocharged");
        assertNode("Cars/Sports/car[2]/driver", "name=Tony Stewart");
        assertNode("Cars/Sports/car[2]", "model=G37", "category=My Sedan");
        assertNode("Cars/Sports/car[3]");
        assertNode("Cars/Sports/car[3]/jcr:xmltext", "jcr:xmlcharacters=This is my text ");
        assertNode("Cars/Sports/car[3]/jcr:xmltext[2]", "jcr:xmlcharacters=that should be merged");
    }

    @Test
    public void shouldParseXmlDocumentWithMixins() throws Exception {
        parse("xmlImport/docWithMixins.xml");
        assertNode("cars", "jcr:mixinTypes=mix:created,mix:lastModified");
    }

    @Test
    public void shouldParseXmlDocumentWithCustomPrimaryType() throws Exception {
        parse("xmlImport/docWithCustomType.xml");
        assertNode("folder", "jcr:mixinTypes=mix:created,mix:lastModified", "jcr:primaryType=nt:folder");
        assertNode("folder/file1", "jcr:primaryType=nt:file");
        assertNode("folder/file1/jcr:content");
        assertNode("folder/file2", "jcr:primaryType=nt:file");
        assertNode("folder/file2/jcr:content");
    }

    @Test
    @FixFor("MODE-1788")
    public void shouldParseXmlDocumentWithMixinsCustomSeparator() throws Exception {
        parse("xmlImport/docWithMixinsCustomSeparator.xml");
        assertNode("cars", "jcr:mixinTypes=mix:created,mix:lastModified");
    }

    @Test
    @FixFor("MODE-1788")
    public void shouldParseXmlDocumentWithPropertiesCustomSeparator() throws Exception {
        parse("xmlImport/docWithPropertiesCustomSeparator.xml");
        assertImportElementWithStrings("story", ";",
                                       "title=Story with commas in the text lead and body text",
                                       "lead=Lead text in attribute, split with a comma.",
                                       "body=Body text in sub element, split by the comma.",
                                       "multiBody=Body text in sub element; split by semicolon");
    }

    @Test
    @FixFor( "MODE-2241" )
    public void shouldParseXmlDocumentWithReferences() throws Exception {
        parse("xmlImport/docWithReferences.xml");
        assertImportElementWithReferences("node1", "hard_ref11=/node2", "hard_ref12=/node2,/node3");
        assertImportElementWithReferences("node2", "hard_ref21=/node1", "hard_ref22=/node1,/node3");

        assertImportElementWithWeakReferences("node1", "weak_ref11=/node2", "weak_ref12=/node2");
        assertImportElementWithWeakReferences("node2", "weak_ref21=/node1", "weak_ref22=/node1");

        assertImportElementWithSimpleReferences("node1", "simple_ref11=/node2", "simple_ref12=/node2");
        assertImportElementWithSimpleReferences("node2", "simple_ref21=/node1", "simple_ref22=/node1");
    }

    @Test
    @FixFor( "MODE-2241" )
    public void shouldParseXmlDocumentWithPropertyTypes() throws Exception {
        parse("xmlImport/docWithPropertyTypes.xml");
        assertImportElementWithStrings("node1", "string_prop=string");
        assertImportElementWithBooleans("node1", "boolean_prop=true");
        assertImportElementWithDecimals("node1", "decimal_prop=12.3");
        assertImportElementWithDoubles("node1", "double_prop=12.3");
        assertImportElementWithLongs("node1", "long_prop=123456");
        assertImportElementWithDates("node1", "date_prop=1994-11-05T13:15:30Z");
        assertImportElementWithNames("node1", "name_prop=nt:undefined");
        assertImportElementWithURIs("node1", "uri_prop=http://www.google.com");
        assertImportElementWithPaths("node1", "path_prop=/a/b/c");
        assertImportElementWithBinaries("node1", "binary_prop=io/file1.txt");
        assertImportElementWithReferences("node1", "reference_prop=/node2");
        assertImportElementWithWeakReferences("node1", "weakreference_prop=/node2");
        assertImportElementWithSimpleReferences("node1", "simplereference_prop=/node2");
    }

    private void assertNode( String path,
                             String... expectedProperties ) {
        assertImportElementWithStrings(path, NodeImportXmlHandler.DEFAULT_MULTI_VALUE_SEPARATOR, expectedProperties);
    }

    private void assertImportElementWithReferences( String path, String... expectedReferences ) {
        NodeImportXmlHandler.ImportElement element = assertImportElementExists(path);
        assertImportElementHasProperties(element,
                                         NodeImportXmlHandler.DEFAULT_MULTI_VALUE_SEPARATOR,
                                         org.modeshape.jcr.value.PropertyType.REFERENCE,
                                         expectedReferences);
    }

    private void assertImportElementWithWeakReferences( String path, String... expectedReferences ) {
        NodeImportXmlHandler.ImportElement element = assertImportElementExists(path);
        assertImportElementHasProperties(element,
                                         NodeImportXmlHandler.DEFAULT_MULTI_VALUE_SEPARATOR,
                                         org.modeshape.jcr.value.PropertyType.WEAKREFERENCE,
                                         expectedReferences);
    }

    private void assertImportElementWithSimpleReferences( String path, String... expectedReferences ) {
        NodeImportXmlHandler.ImportElement element = assertImportElementExists(path);
        assertImportElementHasProperties(element,
                                         NodeImportXmlHandler.DEFAULT_MULTI_VALUE_SEPARATOR,
                                         org.modeshape.jcr.value.PropertyType.SIMPLEREFERENCE,
                                         expectedReferences);
    }

    private void assertImportElementWithBooleans( String path, String... expectedProperties ) {
        NodeImportXmlHandler.ImportElement element = assertImportElementExists(path);
        assertImportElementHasProperties(element,
                                         NodeImportXmlHandler.DEFAULT_MULTI_VALUE_SEPARATOR,
                                         org.modeshape.jcr.value.PropertyType.BOOLEAN,
                                         expectedProperties);
    }

    private void assertImportElementWithDoubles( String path, String... expectedProperties ) {
        NodeImportXmlHandler.ImportElement element = assertImportElementExists(path);
        assertImportElementHasProperties(element,
                                         NodeImportXmlHandler.DEFAULT_MULTI_VALUE_SEPARATOR,
                                         org.modeshape.jcr.value.PropertyType.DOUBLE,
                                         expectedProperties);
    }

    private void assertImportElementWithDecimals( String path, String... expectedProperties ) {
        NodeImportXmlHandler.ImportElement element = assertImportElementExists(path);
        assertImportElementHasProperties(element,
                                         NodeImportXmlHandler.DEFAULT_MULTI_VALUE_SEPARATOR,
                                         org.modeshape.jcr.value.PropertyType.DECIMAL,
                                         expectedProperties);
    }

    private void assertImportElementWithDates( String path, String... expectedProperties ) {
        NodeImportXmlHandler.ImportElement element = assertImportElementExists(path);
        assertImportElementHasProperties(element,
                                         NodeImportXmlHandler.DEFAULT_MULTI_VALUE_SEPARATOR,
                                         org.modeshape.jcr.value.PropertyType.DATE,
                                         expectedProperties);
    }

    private void assertImportElementWithLongs( String path, String... expectedProperties ) {
        NodeImportXmlHandler.ImportElement element = assertImportElementExists(path);
        assertImportElementHasProperties(element,
                                         NodeImportXmlHandler.DEFAULT_MULTI_VALUE_SEPARATOR,
                                         org.modeshape.jcr.value.PropertyType.LONG,
                                         expectedProperties);
    }

    private void assertImportElementWithURIs( String path, String... expectedProperties ) {
        NodeImportXmlHandler.ImportElement element = assertImportElementExists(path);
        assertImportElementHasProperties(element,
                                         NodeImportXmlHandler.DEFAULT_MULTI_VALUE_SEPARATOR,
                                         org.modeshape.jcr.value.PropertyType.URI,
                                         expectedProperties);
    }

    private void assertImportElementWithNames( String path, String... expectedProperties ) {
        NodeImportXmlHandler.ImportElement element = assertImportElementExists(path);
        assertImportElementHasProperties(element,
                                         NodeImportXmlHandler.DEFAULT_MULTI_VALUE_SEPARATOR,
                                         org.modeshape.jcr.value.PropertyType.NAME,
                                         expectedProperties);
    }

    private void assertImportElementWithPaths( String path, String... expectedProperties ) {
        NodeImportXmlHandler.ImportElement element = assertImportElementExists(path);
        assertImportElementHasProperties(element,
                                         NodeImportXmlHandler.DEFAULT_MULTI_VALUE_SEPARATOR,
                                         org.modeshape.jcr.value.PropertyType.PATH,
                                         expectedProperties);
    }

    private void assertImportElementWithBinaries( String path, String... expectedProperties ) {
        NodeImportXmlHandler.ImportElement element = assertImportElementExists(path);
        assertImportElementHasProperties(element,
                                         NodeImportXmlHandler.DEFAULT_MULTI_VALUE_SEPARATOR,
                                         org.modeshape.jcr.value.PropertyType.BINARY,
                                         expectedProperties);
    }

    private void assertImportElementWithStrings( String path,
                                                 String multiValueSeparator,
                                                 String... expectedProperties ) {
        NodeImportXmlHandler.ImportElement element = assertImportElementExists(path);
        assertImportElementHasProperties(element, multiValueSeparator, org.modeshape.jcr.value.PropertyType.STRING, expectedProperties);
    }

    private void assertImportElementHasProperties( NodeImportXmlHandler.ImportElement element,
                                                   String multiValueSeparator,
                                                   org.modeshape.jcr.value.PropertyType expectedPropertyType,
                                                   String... expectedProperties ) {
        for (String propertyValueString : expectedProperties) {
            String[] parts = propertyValueString.split("=");
            String propertyName = context.getValueFactories()
                                         .getNameFactory()
                                         .create(parts[0])
                                         .getString(NoOpEncoder.getInstance());
            String propertyValue = parts[1];
            org.modeshape.jcr.value.PropertyType propertyType = element.getPropertyType(propertyName);

            if (propertyName.equals(JcrConstants.JCR_PRIMARY_TYPE)) {
                assertEquals(propertyValue, element.getType());
            } else {
                Collection<String> actualPropertyValue = propertyName.equalsIgnoreCase(JcrConstants.JCR_MIXIN_TYPES)
                                                         ? element.getMixins() : element.getProperties().get(propertyName);
                assertNotNull(actualPropertyValue);
                String[] values = propertyValue.split(multiValueSeparator);
                for (String value : values) {
                    assertTrue("Expected property not found: " + value, actualPropertyValue.contains(value));
                }
                assertEquals("Invalid property type: " + propertyType, expectedPropertyType, propertyType);
            }
        }
    }

    private NodeImportXmlHandler.ImportElement assertImportElementExists( String path ) {
        Path expectedPath = context.getValueFactories().getPathFactory().create("/" + path);

        NodeImportXmlHandler.ImportElement element = parseResults.get(expectedPath);
        assertNotNull(path + " not found among parsed elements", element);
        return element;
    }

    private void parse( String relativePathToXmlFile ) throws Exception {
        InputStream stream = getClass().getClassLoader().getResourceAsStream(relativePathToXmlFile);
        try {
            XMLReader reader = XMLReaderFactory.createXMLReader();
            reader.setContentHandler(handler);
            reader.setErrorHandler(handler);
            reader.parse(new InputSource(stream));
        } finally {
            if (stream != null) {
                stream.close();
            }
        }
    }

}
