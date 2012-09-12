package org.infinispan.schematic.internal;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import org.infinispan.Cache;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.schematic.SchemaLibrary;
import org.infinispan.schematic.SchemaLibrary.Results;
import org.infinispan.schematic.SchemaValidationTest;
import org.infinispan.schematic.SchematicEntry;
import org.infinispan.schematic.SchematicEntry.FieldName;
import org.infinispan.schematic.document.Document;
import org.infinispan.schematic.document.Json;
import org.infinispan.schematic.internal.document.BasicDocument;
import org.infinispan.test.TestingUtil;
import org.infinispan.test.fwk.TestCacheManagerFactory;
import org.infinispan.transaction.lookup.DummyTransactionManagerLookup;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class CacheSchematicDbTest {

    private CacheSchematicDb db;
    private Cache<String, SchematicEntry> cache;
    private EmbeddedCacheManager cm;

    // private TransactionManager docTm;
    // private TransactionManager schemaTm;

    @Before
    public void beforeTest() {
        ConfigurationBuilder configurationBuilder = new ConfigurationBuilder();
        configurationBuilder.invocationBatching()
                            .enable()
                            .transaction()
                            .transactionManagerLookup(new DummyTransactionManagerLookup());

        cm = TestCacheManagerFactory.createCacheManager(configurationBuilder);
        cache = cm.getCache("documents");
        // tm = TestingUtil.getTransactionManager(cache);
        // Now create the SchematicDb ...
        db = new CacheSchematicDb(cache);
    }

    @After
    public void afterTest() {
        TestingUtil.killCacheManagers(cm);
        cache = null;
        db = null;
        // tm = null;
    }

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
        BasicDocument doc = new BasicDocument();
        doc.put("k1", "value1");
        doc.put("k2", 2);
        String key = "can be anything";
        SchematicEntry prior = db.put(key, doc, null);
        assert prior == null : "Should not have found a prior entry";
        SchematicEntry entry = db.get(key);
        assert entry != null : "Should have found the entry";
        Document read = entry.getContentAsDocument();
        assert read != null;
        assert "value1".equals(read.getString("k1"));
        assert 2 == read.getInteger("k2");
        assert entry.getContentAsBinary() == null : "Should not have a Binary value for the entry's content";
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
        assert !results.hasProblems() : "There are validation problems: " + results;

        SchematicEntry actualEntry = db.get(key);
        Document actualMetadata = actualEntry.getMetadata();
        Document actualDocument = actualEntry.getContentAsDocument();
        assert actualMetadata != null;
        assert actualDocument != null;
        assert schemaUri.equals(actualMetadata.getString(FieldName.SCHEMA_URI)) : "The $schema in the metadata doesn't match: "
                                                                                  + metadata;
        assert key.equals(actualMetadata.getString(FieldName.ID)) : "The id in the metadata doesn't match: " + metadata;

        // Validate just the document ...
        results = db.validate(key);
        assert !results.hasProblems() : "There are validation problems: " + results;

        // Now validate the whole database ...
        Map<String, Results> resultsByKey = db.validate(key, "non-existant");
        assert resultsByKey != null;
        assert !resultsByKey.containsKey(key) : "There are validation problems: " + resultsByKey.get(key);

        // Now validate the whole database ...
        resultsByKey = db.validateAll();
        assert resultsByKey != null;
        assert !resultsByKey.containsKey(key) : "There are validation problems: " + resultsByKey.get(key);
    }

}
