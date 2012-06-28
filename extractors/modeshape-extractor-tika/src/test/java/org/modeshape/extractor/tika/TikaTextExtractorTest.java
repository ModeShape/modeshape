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

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.modeshape.common.util.IoUtil;
import org.modeshape.jcr.InMemoryTestBinary;
import org.modeshape.jcr.api.mimetype.MimeTypeDetector;
import org.modeshape.jcr.mimetype.MimeTypeDetectors;
import org.modeshape.jcr.text.DefaultTextExtractorOutput;
import org.modeshape.jcr.text.TextExtractorContext;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Unit test for {@link TikaTextExtractor}
 */
public class TikaTextExtractorTest {

    private TikaTextExtractor extractor;
    private MimeTypeDetector mimeTypeDetector;
    private LinkedList<String> extracted = null;
    private LinkedList<String> expected = null;

    @Before
    public void beforeEach() {
        extractor = new TikaTextExtractor();
        mimeTypeDetector = new MimeTypeDetectors();
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
        assertThat(extractor.supportsMimeType(mimeTypeOf("modeshape.txt")), is(true));
    }

    @Test
    public void shouldSupportExtractingFromPdfFiles() throws Exception {
        assertThat(extractor.supportsMimeType(mimeTypeOf("modeshape.pdf")), is(true));
    }

    @Test
    public void shouldNotSupportExtractingFromPostscriptFiles() throws Exception {
        assertThat(extractor.supportsMimeType(mimeTypeOf("modeshape.ps")), is(false));
    }

    @Test
    public void shouldSupportExtractingFromDocWordFiles() throws Exception {
        assertThat(extractor.supportsMimeType(mimeTypeOf("modeshape.doc")), is(true));
    }

    @Test
    public void shouldSupportExtractingFromDocxWordFiles() throws Exception {
        assertThat(extractor.supportsMimeType(mimeTypeOf("modeshape.docx")), is(true));
    }

    @Test
    public void shouldExtractTextFromTextFile() throws Exception {
        extractTermsFrom("modeshape.txt");
        loadExpectedFrom("modeshape.txt");
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
    public void shouldExtractTextFromPdfFileGS() throws Exception{
        extractTermsFrom("modeshape_gs.pdf");
        assertExtractedMatchesExpected();
    }

    @Test
    @Ignore( "Exposes the Tika/PDF box bug that characters get duplicated when parsing pdfs produced by PDF Context" )
    public void shouldExtractTextFromPdfFilePdfContext() throws Exception {
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

    private List<String> remainingExpectedTerms() {
        return expected;
    }

    private void extractedShouldHave( String... words ) {
        for (String word : words) {
            assertThat(extracted.pop(), is(word));
        }
    }

    private void extractedShouldHave( List<String> words ) {
        for (String word : words) {
            assertThat(extracted.pop(), is(word));
        }
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
        try {
            TextExtractorContext context = new TextExtractorContext(resourcePath, mimeTypeOf(resourcePath));
            DefaultTextExtractorOutput output = new DefaultTextExtractorOutput();
            extractor.extractFrom(stream, output, context);
            output.toString();
            addWords(extracted, output.getText());
        } finally {
            stream.close();
        }
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

    private String mimeTypeOf( String fileName ) throws Exception {
        return mimeTypeDetector.mimeTypeOf(fileName, new InMemoryTestBinary(getClass().getClassLoader().getResourceAsStream(fileName)));
    }

}
