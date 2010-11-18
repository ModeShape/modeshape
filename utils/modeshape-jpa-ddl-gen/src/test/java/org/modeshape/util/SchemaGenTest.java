package org.modeshape.util;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import java.io.File;
import java.io.IOException;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.connector.store.jpa.JpaSource.Models;

public class SchemaGenTest {

    public static final String MODEL_NAME = Models.SIMPLE.getName();

    private static final File outputPath = new File("target");

    @Before
    public void beforeEach() {
        cleanFiles();
    }

    @AfterClass
    // Run this after the last test to keep the target directory clean
    public static void cleanFiles() {
        new File(outputPath, SchemaGen.CREATE_FILE_NAME).delete();
        new File(outputPath, SchemaGen.DROP_FILE_NAME).delete();
    }

    private void checkFiles() {
        File createFile = new File(outputPath, SchemaGen.CREATE_FILE_NAME);
        assertThat(createFile.exists(), is(true));
        assertThat(createFile.length(), greaterThan(0L));

        File dropFile = new File(outputPath, SchemaGen.DROP_FILE_NAME);
        assertThat(dropFile.exists(), is(true));
        assertThat(dropFile.length(), greaterThan(0L));
    }

    @Test
    public void shouldCreateSchemaForHSqlDialect() throws IOException {
        SchemaGen main = new SchemaGen("org.hibernate.dialect.HSQLDialect", MODEL_NAME, outputPath);
        main.generate();
        checkFiles();
    }

    @Test
    public void shouldCreateSchemaForOracleDialect() throws IOException {
        SchemaGen main = new SchemaGen("org.hibernate.dialect.Oracle10gDialect", MODEL_NAME, outputPath);
        main.generate();
        checkFiles();
    }

    @Test
    public void shouldCreateSchemaForDialectThatIsNotFullyQualified() throws IOException {
        SchemaGen main = new SchemaGen("HSQLDialect", MODEL_NAME, outputPath);
        main.generate();
        checkFiles();
    }

    @Test
    public void shouldCreateSchemaForShortFormDialect() throws IOException {
        SchemaGen main = new SchemaGen("HSQL", MODEL_NAME, outputPath);
        main.generate();
        checkFiles();
    }
    
    @Test
    public void shouldCreateSchemaForOracleDialectWithDelimiter() throws IOException {
        SchemaGen main = new SchemaGen("org.hibernate.dialect.Oracle10gDialect", MODEL_NAME, outputPath, ";");
        main.generate();
        checkFiles();
    }
    
    @Test
    public void shouldCreateSchemaForDialectThatIsNotFullyQualifiedWithDelimiter() throws IOException {
        SchemaGen main = new SchemaGen("HSQLDialect", MODEL_NAME, outputPath, "%");
        main.generate();
        checkFiles();
    }    

}
