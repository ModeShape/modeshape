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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.modeshape.common.FixFor;
import org.modeshape.common.util.FileUtil;
import org.modeshape.common.util.IoUtil;
import org.modeshape.jcr.InMemoryTestBinary;
import org.modeshape.jcr.mimetype.MimeTypeDetector;
import org.modeshape.jcr.mimetype.MimeTypeDetectors;
import org.modeshape.jcr.text.TextExtractorContext;
import org.modeshape.jcr.text.TextExtractorOutput;
import static junit.framework.Assert.assertEquals;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Unit test for {@link TikaTextExtractor}
 */
public class TikaTextExtractorTest {

    private static final MimeTypeDetector DETECTOR = new MimeTypeDetectors();
    private static final int DEFAULT_TIKA_WRITE_LIMIT = 100000;
    private static final String CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final Random RANDOM = new Random();

    private TikaTextExtractor extractor;
    private LinkedList<String> extracted = null;
    private LinkedList<String> expected = null;


    @Before
    public void beforeEach() {
        extractor = new TikaTextExtractor();
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
    public void shouldSupportExtractingFromTextFiles() throws Exception {
        assertThat(extractor.supportsMimeType("text/plain"), is(true));
    }

    @Test
    public void shouldSupportExtractingFromPdfFiles() throws Exception {
        assertThat(extractor.supportsMimeType("application/pdf"), is(true));
    }

    @Test
    public void shouldNotSupportExtractingFromPostscriptFiles() throws Exception {
        assertThat(extractor.supportsMimeType("application/postscript"), is(false));
    }

    @Test
    public void shouldSupportExtractingFromDocWordFiles() throws Exception {
        assertThat(extractor.supportsMimeType("application/msword"), is(true));
    }

    @Test
    public void shouldSupportExtractingFromDocxWordFiles() throws Exception {
        assertThat(extractor.supportsMimeType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
                   is(true));
    }

    @Test
    public void shouldExtractTextFromTextFile1() throws Exception {
        extractTermsFrom("modeshape.txt");
        loadExpectedFrom("modeshape.txt");
        extractedShouldHave(remainingExpectedTerms());
    }

    @Test
    public void shouldExtractTextFromTextFile2() throws Exception {
        extractTermsFrom("text-file.txt");
        loadExpectedFrom("text-file.txt");
        extractedShouldHave(remainingExpectedTerms());
    }

    @Test
    public void shouldExtractTextFromDocFile() throws Exception {
        extractTermsFrom("modeshape.doc");
        loadExpectedFrom("modeshape.txt");
        extractedShouldHave(remainingExpectedTerms());
    }

    @Test
    public void shouldExtractTextFromDocxFile() throws Exception {
        extractTermsFrom("modeshape.docx");
        loadExpectedFrom("modeshape.txt");
    }

    @Test
    public void shouldExtractTextFromPdfFileGS() throws Exception {
        extractTermsFrom("modeshape_gs.pdf");
        assertExtractedMatchesExpected();
    }

    @Test
    @FixFor( "MODE-1561" )
    public void shouldExtractUsingWriteLimit() throws Exception {
        int stringLength = DEFAULT_TIKA_WRITE_LIMIT + 2;
        String rndString = randomString(stringLength);

        File tempFile = File.createTempFile("tika_extraction_",  ".txt");
        try {
            IoUtil.write(rndString, tempFile);

            extractor.setWriteLimit(stringLength);
            TextExtractorOutput output = new TextExtractorOutput();
            extractor.extractFrom(new InMemoryTestBinary(new FileInputStream(tempFile)), output, new TextExtractorContext(DETECTOR));

            assertEquals(rndString, output.getText());
        } finally {
            FileUtil.delete(tempFile);
        }
    }

    @Test
    @Ignore( "Exposes the Tika/PDF box bug that characters get duplicated when parsing pdfs produced by PDF Context" )
    public void shouldExtractTextFromPdfFilePdfContext() throws Exception {
        extractTermsFrom("modeshape_pdfcontext.pdf");
        assertExtractedMatchesExpected();
    }

    @Test
    @FixFor( "MODE-1810" )
    public void shouldExtractTextFromXlsxFile() throws Exception {
        extractTermsFrom("sample-file.xlsx");
        assertTrue(!extracted.isEmpty());
    }

    public static String randomString(int length) {
        //write a text only header to make sure Tika Mimetype detector doesn't get confused...
        String header = "this is a text file ";
        StringBuilder rndStringBuilder = new StringBuilder(length);
        rndStringBuilder.append(header);
        for (int i = 0; i < length - header.length(); i++) {
            rndStringBuilder.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
        }
        return rndStringBuilder.toString();
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

    private List<String> remainingExpectedTerms() {
        return expected;
    }

    private void extractedShouldHave( String... words ) {
        for (String word : words) {
            assertThat(extracted.pop(), is(word));
        }
    }

    private void extractedShouldHave( List<String> words ) {
        List<String> missingWords = new LinkedList<String>();
        for (String word : words) {
            String extractedWord = null;
            try {
                extractedWord = extracted.pop();
            } catch (NoSuchElementException e) {
                missingWords.add(word);
                continue;
            }
            assertThat(extractedWord, is(word));
        }
        assertThat("Missing words: " + missingWords, missingWords.size(), is(0));
    }

    private List<String> expectedTermsThrough( String... words ) {
        if (words == null || words.length == 0) {
            return Collections.emptyList();
        }
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
                if (foundAll) {
                    return result;
                }
            }
        }
        return result;
    }

    private void extractTermsFrom( String resourcePath ) throws Exception {
        InputStream stream = getClass().getClassLoader().getResourceAsStream(resourcePath);
        assertThat(stream, is(notNullValue()));
        TextExtractorOutput output = new TextExtractorOutput();
        extractor.extractFrom(new InMemoryTestBinary(stream), output, new TextExtractorContext(DETECTOR));
        output.toString();
        addWords(extracted, output.getText());
    }

    private void loadExpectedFrom( String resourcePath ) throws IOException {
        InputStream stream = getClass().getClassLoader().getResourceAsStream(resourcePath);
        assertThat(stream, is(notNullValue()));
        try {
            addWords(expected, IoUtil.read(stream));
        } finally {
            stream.close();
        }
    }

    private void addWords( List<String> words,
                           String input ) {
        for (String word : input.split("[\\s\"]+")) {
            if (word.length() > 0) {
                words.add(word);
            }
        }
    }
}
