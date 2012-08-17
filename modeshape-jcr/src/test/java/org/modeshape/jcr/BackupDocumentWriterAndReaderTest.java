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
