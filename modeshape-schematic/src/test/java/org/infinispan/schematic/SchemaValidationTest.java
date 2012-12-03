package org.infinispan.schematic;

import java.io.InputStream;
import org.infinispan.schematic.document.Document;
import org.infinispan.schematic.document.Json;
import org.infinispan.schematic.internal.InMemoryDocumentLibrary;
import org.infinispan.schematic.internal.document.Paths;
import org.infinispan.schematic.internal.schema.SchemaDocument;
import org.infinispan.schematic.internal.schema.SchemaDocumentCache;
import org.infinispan.schematic.internal.schema.ValidationResult;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

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
