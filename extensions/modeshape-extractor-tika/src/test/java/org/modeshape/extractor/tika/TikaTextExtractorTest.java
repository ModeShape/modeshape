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
package org.modeshape.extractor.tika;

import org.apache.tika.exception.TikaException;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.modeshape.common.FixFor;
import org.modeshape.common.collection.Problems;
import org.modeshape.common.collection.SimpleProblems;
import org.modeshape.common.util.IoUtil;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.Property;
import org.modeshape.graph.text.TextExtractorContext;
import org.modeshape.graph.text.TextExtractorOutput;
import org.xml.sax.SAXException;

public class TikaTextExtractorTest {

    private static final int DEFAULT_TIKA_WRITE_LIMIT = 100000;
    private static final String CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final Random RANDOM = new Random();

    private TikaTextExtractor extractor;
    private ExecutionContext execContext;
    private Path inputPath;
    private Set<Property> inputProperties;
    private String mimeType;
    private Problems problems;
    private boolean print = false;
    private LinkedList<String> extracted = null;
    private LinkedList<String> expected = null;

    @Before
    public void beforeEach() {
        execContext = new ExecutionContext();
        extractor = new TikaTextExtractor();
        inputProperties = new HashSet<Property>();
        print = false;
        extracted = new LinkedList<String>();
        expected = new LinkedList<String>();
    }

    @Test
    public void shouldIncludedNoMimeTypesByDefault() {
        assertThat(extractor.getIncludedMimeTypes().isEmpty(), is(true));
    }

    @Test
    public void shouldExcludedPackageTypeMimeTypesByDefault() {
        assertThat(extractor.getExcludedMimeTypes().containsAll(TikaTextExtractor.DEFAULT_EXCLUDED_MIME_TYPES), is(true));
    }

    @Test
    public void shouldSupportExtractingFromTextFiles() throws IOException {
        assertThat(extractor.supportsMimeType(mimeTypeOf("modeshape.txt")), is(true));
    }

    @Test
    public void shouldSupportExtractingFromPdfFiles() throws IOException {
        assertThat(extractor.supportsMimeType(mimeTypeOf("modeshape.pdf")), is(true));
    }

    @Test
    public void shouldNotSupportExtractingFromPostscriptFiles() throws IOException {
        assertThat(extractor.supportsMimeType(mimeTypeOf("modeshape.ps")), is(false));
    }

    @Test
    public void shouldSupportExtractingFromDocWordFiles() throws IOException {
        assertThat(extractor.supportsMimeType(mimeTypeOf("modeshape.doc")), is(true));
    }

    @Test
    public void shouldSupportExtractingFromDocxWordFiles() throws IOException {
        assertThat(extractor.supportsMimeType(mimeTypeOf("modeshape.docx")), is(true));
    }

    @Test
    public void shouldExtractTextFromTextFile() throws IOException {
        extractTermsFrom("modeshape.txt");
        loadExpectedFrom("modeshape.txt");
        extractedShouldHave(remainingExpectedTerms());
    }

    @Test
    public void shouldExtractTextFromDocFile() throws IOException {
        extractTermsFrom("modeshape.doc");
        loadExpectedFrom("modeshape.txt");
        extractedShouldHave(remainingExpectedTerms());
    }

    @Test
    public void shouldExtractTextFromDocxFile() throws IOException {
        extractTermsFrom("modeshape.docx");
        loadExpectedFrom("modeshape.txt");
    }

    @Test
    public void shouldExtractTextFromPdfFileGS() throws IOException, SAXException, TikaException {
        extractTermsFrom("modeshape_gs.pdf");
        assertExtractedMatchesExpected();
    }

    @Test
    @FixFor( "MODE-1561" )
    public void shouldNotExtractPastDefaultTikaWriteLimit() throws IOException {
        String rndString = randomString(DEFAULT_TIKA_WRITE_LIMIT + 1);

        File tempFile = File.createTempFile("tika_extraction_",  ".txt");
        tempFile.deleteOnExit();
        IoUtil.write(rndString, tempFile);

        SimpleProblems problems = new SimpleProblems();
        TextExtractorContext context = new TextExtractorContext(execContext, path(tempFile.getName()), inputProperties, "text/plain",
                                                                problems);

        extractor.extractFrom(new FileInputStream(tempFile), new StringTextExtractorOutput(), context);

        assertEquals(1, problems.size());
    }

    @Test
    @FixFor( "MODE-1561" )
    public void shouldExtractPastTikaWriteLimitIfConfigured() throws Exception {
        int stringLength = DEFAULT_TIKA_WRITE_LIMIT + 1;
        String rndString = randomString(stringLength);

        File tempFile = File.createTempFile("tika_extraction_",  ".txt");
        tempFile.deleteOnExit();
        IoUtil.write(rndString, tempFile);

        SimpleProblems problems = new SimpleProblems();
        TextExtractorContext context = new TextExtractorContext(execContext, path(tempFile.getName()), inputProperties, "text/plain",
                                                                problems);
        StringTextExtractorOutput output = new StringTextExtractorOutput();

        extractor.setWriteLimit(stringLength);
        extractor.extractFrom(new FileInputStream(tempFile), output, context);

        assertTrue(problems.toString(), problems.isEmpty());
        assertEquals(rndString, output.toString());
    }

    public static String randomString(int length) {
        StringBuilder rndStringBuilder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            rndStringBuilder.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
        }
        return rndStringBuilder.toString();
    }

    @Test
    @Ignore("Exposes the Tika/PDF box bug that characters get duplicated when parsing pdfs produced by PDF Context")
    public void shouldExtractTextFromPdfFilePdfContext() throws IOException, SAXException, TikaException {
        extractTermsFrom("modeshape_pdfcontext.pdf");
        assertExtractedMatchesExpected();
    }

    private void assertExtractedMatchesExpected() throws IOException {
        loadExpectedFrom("modeshape.txt");
        extractedShouldHave("2011-01-24");
        extractedShouldHave("-", "1/2", "-");
        extractedShouldHave(expectedTermsThrough("-", "versioning"));
        extractedShouldHave("2011-01-24");
        extractedShouldHave("-", "2/2", "-");
        extractedShouldHave(remainingExpectedTerms());
    }

    protected Path path( String path ) {
        return execContext.getValueFactories().getPathFactory().create(path);
    }

    protected List<String> remainingExpectedTerms() {
        return expected;
    }

    protected void extractedShouldHave( String... words ) {
        for (String word : words) {
            assertThat(extracted.pop(), is(word));
        }
    }

    protected void extractedShouldHave( List<String> words ) {
        for (String word : words) {
            assertThat(extracted.pop(), is(word));
        }
    }

    protected List<String> expectedTermsThrough( String... words ) {
        if (words == null || words.length == 0) return Collections.emptyList();
        LinkedList<String> result = new LinkedList<String>();
        String nextWord = words[0];
        while (nextWord != null && !expected.isEmpty()) {
            String word = expected.pop();
            result.add(word);
            if (word.equals(nextWord)) {
                boolean foundAll = true;
                for (int i = 1; i != words.length; ++i) {
                    String next = expected.pop();
                    result.add(next);
                    if (!next.equals(words[i])) {
                        foundAll = false;
                        break;
                    }
                }
                if (foundAll) return result;
            }
        }
        System.out.println("expected terms thru " + words + " are: " + result);
        return result;
    }

    protected void extractTermsFrom( String resourcePath ) throws IOException {
        InputStream stream = getClass().getClassLoader().getResourceAsStream(resourcePath);
        assertThat(stream, is(notNullValue()));
        try {
            if (inputPath == null) inputPath = path(resourcePath);
            if (mimeType == null) mimeType = execContext.getMimeTypeDetector().mimeTypeOf(resourcePath, null);
            if (problems == null) problems = new SimpleProblems();
            TextExtractorContext context = new TextExtractorContext(execContext, inputPath, inputProperties, mimeType, problems);
            TextExtractorOutput output = new StringTextExtractorOutput();
            extractor.extractFrom(stream, output, context);
            String result = output.toString();
            if (print) {
                System.out.println("Text extracted from \"" + resourcePath + "\"");
                System.out.println("============================================");
                System.out.println(result);
            }
            if (!problems.isEmpty()) {
                System.out.println(problems);
                assertThat(problems.size(), is(0));
            }
            addWords(extracted, output.toString());
        } finally {
            stream.close();
        }
    }

    protected void loadExpectedFrom( String resourcePath ) throws IOException {
        InputStream stream = getClass().getClassLoader().getResourceAsStream(resourcePath);
        assertThat(stream, is(notNullValue()));
        try {
            addWords(expected, IoUtil.read(stream));
        } finally {
            stream.close();
        }
    }

    protected void addWords( List<String> words,
                             String input ) {
        for (String word : input.split("[\\s\"]+")) {
            if (word.length() > 0) words.add(word);
        }
    }

    protected String mimeTypeOf( String resourcePath ) throws IOException {
        return execContext.getMimeTypeDetector().mimeTypeOf(resourcePath, null);
    }

}
