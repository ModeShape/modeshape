package org.jboss.dna.util;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import java.io.File;
import java.io.IOException;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

public class SchemaGenTest {

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
        SchemaGen main = new SchemaGen("org.hibernate.dialect.HSQLDialect", "Basic", outputPath);
        main.generate();
        checkFiles();
    }

    @Test
    public void shouldCreateSchemaForOracleDialect() throws IOException {
        SchemaGen main = new SchemaGen("org.hibernate.dialect.Oracle10gDialect", "Basic", outputPath);
        main.generate();
        checkFiles();
    }

    @Test
    public void shouldCreateSchemaForDialectThatIsNotFullyQualified() throws IOException {
        SchemaGen main = new SchemaGen("HSQLDialect", "Basic", outputPath);
        main.generate();
        checkFiles();
    }

    @Test
    public void shouldCreateSchemaForShortFormDialect() throws IOException {
        SchemaGen main = new SchemaGen("HSQL", "Basic", outputPath);
        main.generate();
        checkFiles();
    }

}
