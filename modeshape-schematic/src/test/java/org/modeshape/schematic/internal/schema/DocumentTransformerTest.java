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
package org.modeshape.schematic.internal.schema;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import java.io.InputStream;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.modeshape.schematic.SchemaLibrary;
import org.modeshape.schematic.Schematic;
import org.modeshape.schematic.document.Document;
import org.modeshape.schematic.document.Json;
import org.modeshape.schematic.document.ParsingException;

public class DocumentTransformerTest {

    protected static final String PARTS_SCHEMA_URI = "json/schema/spec-example-doc.json";

    private static SchemaLibrary schemas;
    private static boolean print;

    @BeforeClass
    public static void beforeAll() throws Exception {
        schemas = Schematic.createSchemaLibrary();
        schemas.put("http://json-schema.org/draft-03/schema#", Json.read(resource("json/schema/draft-03/schema.json")));
        schemas.put(PARTS_SCHEMA_URI, Json.read(resource("json/schema/spec-example.json")));
        // schemas.put("json/schema/repository-config-schema.json",
        // Json.read(resource("json/schema/repository-config-schema.json")));
    }

    @Before
    public void beforeEach() {
        print = false;
    }

    @Test
    public void shouldNotTransformDocumentWithNoMismatchedValues() throws Exception {
        Document doc = doc("{ 'name' : 'Acme Bottle Opener', 'id' : 123 , 'price' : 2.99, 'tags' : [ 'easy', 'under-10-dollars' ] }");
        transform(doc, PARTS_SCHEMA_URI, 0);
    }

    @Test
    public void shouldTransformDocumentWithStringValueWhereIntegerExpected() throws Exception {
        // print = true;
        Document doc = doc("{ 'name' : 'Acme Bottle Opener', 'id' : '123' , 'price' : 2.99, 'tags' : [ 'easy', 'under-10-dollars' ] }");
        transform(doc, PARTS_SCHEMA_URI, 1);
    }

    protected static InputStream resource( String resourcePath ) {
        InputStream result = SchemaValidationTest.class.getClassLoader().getResourceAsStream(resourcePath);
        assertNotNull("Could not find resource \"" + resourcePath + "\"", result);
        return result;
    }

    protected static Document doc( String content ) throws ParsingException {
        Document doc = Json.read(content);
        if (print) System.out.println(doc);
        return doc;
    }

    protected static Document transform( Document doc,
                                         String schemaUri,
                                         int numExpectedMismatchedValues ) {
        SchemaLibrary.Results results = schemas.validate(doc, schemaUri);
        if (print) System.out.println(results);
        if (numExpectedMismatchedValues > 0) {
            assertThat("expected mismatch errors, but found none", results.hasOnlyTypeMismatchErrors(), is(true));
            assertThat("expected different number of mismatches", results.errorCount(), is(numExpectedMismatchedValues));
        } else {
            assertThat("expected no mismatch errors", results.hasOnlyTypeMismatchErrors(), is(false));
            assertThat("expected to find problems", results.hasProblems(), is(false));
        }
        Document output = schemas.convertValues(doc, results);
        if (numExpectedMismatchedValues > 0) {
            assertThat(output, is(not(sameInstance(doc))));
            // Now double check that the output is valid ...
            SchemaLibrary.Results newResults = schemas.validate(output, schemaUri);
            assertThat(newResults.hasErrors(), is(false));
            if (print) {
                System.out.println("After converting: " + output);
                System.out.println(newResults);
            }
        } else {
            assertThat(output, is(sameInstance(doc)));
        }
        return output;
    }

}
