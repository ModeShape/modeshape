package org.infinispan.schematic;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import org.infinispan.schematic.SchemaLibrary.Results;
import org.infinispan.schematic.SchematicEntry.FieldName;
import org.infinispan.schematic.document.Document;
import org.infinispan.schematic.document.EditableDocument;
import org.infinispan.schematic.document.Json;
import org.infinispan.schematic.internal.document.BasicDocument;
import org.junit.Test;

public class SchematicDbTest extends AbstractSchematicDbTest {

    protected static InputStream resource( String resourcePath ) {
        InputStream result = SchemaValidationTest.class.getClassLoader().getResourceAsStream(resourcePath);
        assert result != null : "Could not find resource \"" + resourcePath + "\"";
        return result;
    }

    protected void loadSchemas() throws IOException {
        SchemaLibrary schemas = db.getSchemaLibrary();
        schemas.put("http://json-schema.org/draft-03/schema#", Json.read(resource("json/schema/draft-03/schema.json")));
        schemas.put("json/schema/spec-example.json", Json.read(resource("json/schema/spec-example.json")));
    }

    @Test
    public void shouldStoreDocumentWithUnusedKeyAndWithNullMetadata() {
        Document doc = Schematic.newDocument("k1", "value1", "k2", 2);
        String key = "can be anything";
        SchematicEntry prior = db.put(key, doc, null);
        assertThat("Should not have found a prior entry", prior, is(nullValue()));
        SchematicEntry entry = db.get(key);
        assertThat("Should have found the entry", entry, is(notNullValue()));

        // Verify the content ...
        Document read = entry.getContentAsDocument();
        assertThat(read, is(notNullValue()));
        assertThat(read.getString("k1"), is("value1"));
        assertThat(read.getInteger("k2"), is(2));
        assertThat("Should not have a Binary value for the entry's content", entry.getContentAsBinary(), is(nullValue()));
        assertThat(read.containsAll(doc), is(true));
        assertThat(read.equals(doc), is(true));

        // Verify the metadata ...
        Document readMetadata = entry.getMetadata();
        assertThat(readMetadata, is(notNullValue()));
        assertThat(readMetadata.getString("id"), is(key));
    }

    @Test
    public void shouldStoreDocumentWithUnusedKeyAndWithNonNullMetadata() {
        Document doc = Schematic.newDocument("k1", "value1", "k2", 2);
        Document metadata = Schematic.newDocument("mimeType", "text/plain");
        String key = "can be anything";
        SchematicEntry prior = db.put(key, doc, metadata);
        assertThat("Should not have found a prior entry", prior, is(nullValue()));

        // Read back from the database ...
        SchematicEntry entry = db.get(key);
        assertThat("Should have found the entry", entry, is(notNullValue()));

        // Verify the content ...
        Document read = entry.getContentAsDocument();
        assertThat(read, is(notNullValue()));
        assertThat(read.getString("k1"), is("value1"));
        assertThat(read.getInteger("k2"), is(2));
        assertThat("Should not have a Binary value for the entry's content", entry.getContentAsBinary(), is(nullValue()));
        assertThat(read.containsAll(doc), is(true));
        assertThat(read.equals(doc), is(true));

        // Verify the metadata ...
        Document readMetadata = entry.getMetadata();
        assertThat(readMetadata, is(notNullValue()));
        assertThat(readMetadata.getString("mimeType"), is(metadata.getString("mimeType")));
        assertThat(readMetadata.containsAll(metadata), is(true));

        // metadata contains more than what we specified ...
        assertThat("Expected:\n" + metadata + "\nFound: \n" + readMetadata, readMetadata.equals(metadata), is(false));
    }

    @Test
    public void shouldStoreDocumentAndValidateAfterRefetching() throws Exception {
        loadSchemas();
        Document doc = Json.read(resource("json/spec-example-doc.json"));
        String key = "json/spec-example-doc.json";
        String schemaUri = "json/schema/spec-example.json";
        Document metadata = new BasicDocument(FieldName.SCHEMA_URI, schemaUri);
        db.put(key, doc, metadata);
        Results results = db.getSchemaLibrary().validate(doc, schemaUri);
        assertThat("There are validation problems: " + results, results.hasProblems(), is(false));

        SchematicEntry actualEntry = db.get(key);
        Document actualMetadata = actualEntry.getMetadata();
        Document actualDocument = actualEntry.getContentAsDocument();
        assertThat(actualMetadata, is(notNullValue()));
        assertThat(actualDocument, is(notNullValue()));
        assertThat("The $schema in the metadata doesn't match: " + metadata,
                   actualMetadata.getString(FieldName.SCHEMA_URI),
                   is(schemaUri));

        // Validate just the document ...
        results = db.validate(key);
        assertThat("There are validation problems: " + results, results.hasProblems(), is(false));

        // Now validate the whole database ...
        Map<String, Results> resultsByKey = db.validate(key, "non-existant");
        assertThat(resultsByKey, is(notNullValue()));
        assertThat("There are validation problems: " + resultsByKey.get(key), resultsByKey.containsKey(key), is(false));

        // Now validate the whole database ...
        resultsByKey = db.validateAll();
        assertThat(resultsByKey, is(notNullValue()));
        assertThat("There are validation problems: " + resultsByKey.get(key), resultsByKey.containsKey(key), is(false));
    }

    @Test
    public void shouldStoreDocumentAndFetchAndModifyAndRefetch() throws Exception {
        // Store the document ...
        Document doc = Schematic.newDocument("k1", "value1", "k2", 2);
        Document metadata = Schematic.newDocument("mimeType", "text/plain");
        String key = "can be anything";
        SchematicEntry prior = db.put(key, doc, metadata);
        assertThat("Should not have found a prior entry", prior, is(nullValue()));

        // Read back from the database ...
        SchematicEntry entry = db.get(key);
        assertThat("Should have found the entry", entry, is(notNullValue()));

        // Verify the content ...
        Document read = entry.getContentAsDocument();
        assertThat(read, is(notNullValue()));
        assertThat(read.getString("k1"), is("value1"));
        assertThat(read.getInteger("k2"), is(2));
        assertThat("Should not have a Binary value for the entry's content", entry.getContentAsBinary(), is(nullValue()));
        assertThat(read.containsAll(doc), is(true));
        assertThat(read.equals(doc), is(true));

        // Modify using an editor ...
        try {
            // tm.begin();
            EditableDocument editable = entry.editDocumentContent();
            editable.setBoolean("k3", true);
            editable.setNumber("k4", 3.5d);
        } finally {
            // tm.commit();
        }

        // Now re-read ...
        SchematicEntry entry2 = db.get(key);
        Document read2 = entry2.getContentAsDocument();
        assertThat(read2, is(notNullValue()));
        assertThat(read2.getString("k1"), is("value1"));
        assertThat(read2.getInteger("k2"), is(2));
        assertThat(read2.getBoolean("k3"), is(true));
        assertThat(read2.getDouble("k4") > 3.4d, is(true));
    }
}
