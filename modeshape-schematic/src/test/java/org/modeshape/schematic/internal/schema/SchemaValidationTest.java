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

import java.io.InputStream;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.modeshape.schematic.DocumentLibrary;
import org.modeshape.schematic.SchemaLibrary;
import org.modeshape.schematic.document.Document;
import org.modeshape.schematic.document.Json;
import org.modeshape.schematic.internal.InMemoryDocumentLibrary;
import org.modeshape.schematic.internal.document.Paths;

public class SchemaValidationTest {

    private static DocumentLibrary docs;
    private SchemaDocumentCache schemaDocs;
    private SchemaLibrary.Results results;

    @BeforeClass
    public static void beforeAll() throws Exception {
        docs = new InMemoryDocumentLibrary("tests");
        docs.put("http://json-schema.org/draft-03/schema#", Json.read(resource("json/schema/draft-03/schema.json")));
        docs.put("json/spec-example-doc.json", Json.read(resource("json/spec-example-doc.json")));
        docs.put("json/schema/spec-example.json", Json.read(resource("json/schema/spec-example.json")));
        docs.put("json/sample-repo-config.json", Json.read(resource("json/sample-repo-config.json")));
        docs.put("json/empty.json", Json.read(resource("json/empty.json")));
        docs.put("json/schema/repository-config-schema.json", Json.read(resource("json/schema/repository-config-schema.json")));
        docs.put("json/schema/enum-example.json", Json.read(resource("json/schema/enum-example.json")));
        docs.put("json/enum-example-doc.json", Json.read(resource("json/enum-example-doc.json")));
    }

    protected static InputStream resource( String resourcePath ) {
        InputStream result = SchemaValidationTest.class.getClassLoader().getResourceAsStream(resourcePath);
        assert result != null : "Could not find resource \"" + resourcePath + "\"";
        return result;
    }

    @Before
    public void beforeEach() {
        schemaDocs = new SchemaDocumentCache(docs, null);
    }

    protected void assertNoProblems( SchemaLibrary.Results results ) {
        if (results.hasProblems()) {
            System.out.println(results);
            assert false;
        }
    }

    protected SchemaLibrary.Results validate( String documentUri,
                                              String schemaUri ) {
        Document doc = docs.get(documentUri);
        ValidationResult problems = new ValidationResult();
        SchemaDocument schema = schemaDocs.get(schemaUri, problems);
        if (schema != null) {
            schema.getValidator().validate(null, null, doc, Paths.rootPath(), problems, schemaDocs);
        }
        return problems;
    }

    @Test
    public void shouldBeAbleToValidateDocumentUsingSimpleSchemaDocument() throws Exception {
        results = validate("json/spec-example-doc.json", "json/schema/spec-example.json");
        assertNoProblems(results);
    }

    @Test
    public void shouldBeAbleToValidateSampleDocumentUsingSchemaDocument() throws Exception {
        results = validate("json/sample-repo-config.json", "json/schema/repository-config-schema.json");
        assertNoProblems(results);
    }

    @Test
    public void shouldBeAbleToValidateDocumentUsingCaseInsensitiveEnums() throws Exception {
        results = validate("json/enum-example-doc.json", "json/schema/enum-example.json");
        assertNoProblems(results);
    }

    @Test
    public void shouldNotBeAbleToValidateEmptyDocumentUsingSchemaDocument() throws Exception {
        results = validate("json/empty.json", "json/schema/repository-config-schema.json");
        assert results.errorCount() == 1;
    }
}
