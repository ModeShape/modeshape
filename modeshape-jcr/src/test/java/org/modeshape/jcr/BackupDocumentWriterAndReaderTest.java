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
package org.modeshape.jcr;

import static org.hamcrest.core.Is.is;
import static org.infinispan.schematic.Schematic.newDocument;
import static org.junit.Assert.assertThat;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.infinispan.schematic.document.Document;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.modeshape.common.collection.Problems;
import org.modeshape.common.collection.SimpleProblems;
import org.modeshape.common.util.FileUtil;

public class BackupDocumentWriterAndReaderTest {

    private static List<Document> documents;

    private BackupDocumentWriter writer;
    private BackupDocumentReader reader;
    private File testDirectory;
    private Problems problems;

    @BeforeClass
    public static void beforeAll() {
        documents = new ArrayList<Document>();
        documents.add(newDocument("field1", "value1"));
        documents.add(newDocument("field1", "value1", "field2", 1300L, "field3", "other value"));
        documents.add(newDocument("fieldX", "valueX", "fieldY", 238L, "fieldZ", "yet another value", "booleanField", Boolean.TRUE));
    }

    @Before
    public void setUp() throws Exception {
        testDirectory = new File("target/backupArea/writerTests");
        testDirectory.mkdirs();
        FileUtil.delete(testDirectory);
        testDirectory.mkdirs();
        problems = new SimpleProblems();
    }

    @After
    public void tearDown() throws Exception {
        try {
            writer.close();
        } finally {
            try {
                FileUtil.delete(testDirectory);
            } finally {
                problems = null;
                writer = null;
                testDirectory = null;
            }
        }
    }

    protected void useCompression( boolean compression,
                                   int maxDocsPerBackupFile ) {
        writer = new BackupDocumentWriter(testDirectory, "backup", maxDocsPerBackupFile, compression, problems);
        reader = new BackupDocumentReader(testDirectory, "backup", problems);
    }

    protected List<Document> readAllDocuments() {
        List<Document> results = new ArrayList<Document>();
        while (true) {
            Document doc = reader.read();
            if (doc == null) break;
            results.add(doc);
        }
        return results;
    }

    protected void assertNoProblems() {
        if (problems.hasProblems()) {
            System.out.println(problems);
        }
        assertThat(problems.hasProblems(), is(false));
    }

    protected void assertDocuments( Iterable<Document> actual,
                                    Iterable<Document> expected ) {
        Iterator<Document> actualIter = actual.iterator();
        Iterator<Document> expectedIter = expected.iterator();
        while (actualIter.hasNext() && expectedIter.hasNext()) {
            assertThat(actualIter.next().equals(expectedIter.next()), is(true));
        }
        assertThat(actualIter.hasNext(), is(false));
        assertThat(expectedIter.hasNext(), is(false));
    }

    @Test
    public void shouldWriteAndReadSingleFileWithoutCompression() throws Exception {
        useCompression(false, 5);
        // Write one document
        Document doc = documents.get(0);
        writer.write(doc);
        writer.close();
        assertNoProblems();
        // Read all documents ...
        List<Document> readDocs = readAllDocuments();
        assertNoProblems();
        assertThat(readDocs.size(), is(1));
        assertThat(readDocs.get(0).equals(doc), is(true));
    }

    @Test
    public void shouldWriteAndReadMultipleFilesWithoutCompression() throws Exception {
        useCompression(false, 5);
        // Write one document
        for (Document doc : documents) {
            writer.write(doc);
        }
        writer.close();
        assertNoProblems();
        // Read all documents ...
        List<Document> readDocs = readAllDocuments();
        assertNoProblems();
        assertThat(readDocs.size(), is(documents.size()));
        assertDocuments(readDocs, documents);
    }

    @Test
    public void shouldWriteAndReadSingleFileWithCompression() throws Exception {
        useCompression(true, 5);
        // Write one document
        Document doc = documents.get(0);
        writer.write(doc);
        writer.close();
        assertNoProblems();
        // Read all documents ...
        List<Document> readDocs = readAllDocuments();
        assertNoProblems();
        assertThat(readDocs.size(), is(1));
        assertThat(readDocs.get(0).equals(doc), is(true));
    }

    @Test
    public void shouldWriteAndReadMultipleFilesWithCompression() throws Exception {
        useCompression(true, 5);
        // Write one document
        for (Document doc : documents) {
            writer.write(doc);
        }
        writer.close();
        assertNoProblems();
        // Read all documents ...
        List<Document> readDocs = readAllDocuments();
        assertNoProblems();
        assertThat(readDocs.size(), is(documents.size()));
        assertDocuments(readDocs, documents);
    }

    @Test
    public void shouldWriteAndReadMultipleFilesIntoMultipleBackupFilesWithoutCompression() throws Exception {
        useCompression(false, 2);
        // Write one document
        for (Document doc : documents) {
            writer.write(doc);
        }
        writer.close();
        assertNoProblems();
        // Read all documents ...
        List<Document> readDocs = readAllDocuments();
        assertNoProblems();
        assertThat(readDocs.size(), is(documents.size()));
        assertDocuments(readDocs, documents);
    }

    @Test
    public void shouldWriteAndReadMultipleFilesIntoMultipleBackupFilesWithCompression() throws Exception {
        useCompression(true, 2);
        // Write one document
        for (Document doc : documents) {
            writer.write(doc);
        }
        writer.close();
        assertNoProblems();
        // Read all documents ...
        List<Document> readDocs = readAllDocuments();
        assertNoProblems();
        assertThat(readDocs.size(), is(documents.size()));
        assertDocuments(readDocs, documents);
    }
}
