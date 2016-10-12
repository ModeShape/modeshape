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
package org.modeshape.sequencer.pdf;

import java.awt.print.PageFormat;
import java.awt.print.PrinterException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPageable;

/**
 * Utility for extracting Document Information Directory metadata from PDF files.
 */
public class PdfBasicMetadata {

    static final String MIME_TYPE_STRING = "application/pdf";

    static final String[] ORIENTATION_STRINGS = {"landscape", "portrait", "reverse landscape"};

    private Integer pageCount;
    private String author;
    private Calendar creationDate;
    private String creator;
    private String keywords;
    private Calendar modificationDate;
    private String producer;
    private String subject;
    private String title;
    private String orientation;
    private Boolean encrypted;
    private String version;

    private InputStream in;

    /*
     * Check that given file is supported by this sequencer.
     */
    public boolean check() {
        try {
            PDDocument document = PDDocument.load(in);
            PDPageable pageable = new PDPageable(document);
            PageFormat firstPage = pageable.getPageFormat(0);

            encrypted = document.isEncrypted();
            pageCount = document.getNumberOfPages();
            orientation = ORIENTATION_STRINGS[firstPage.getOrientation()];
            version = String.valueOf(document.getDocument().getVersion());
            String catalogVersion = document.getDocumentCatalog().getVersion();
            if (catalogVersion != null && !catalogVersion.isEmpty()) {
                // According to specs version saved here should be determining instead
                // the version in header. It is barely used, though.
                version = catalogVersion;
            }

            if (!encrypted) {
                PDDocumentInformation metadata = document.getDocumentInformation();
                author = metadata.getAuthor();
                creationDate = metadata.getCreationDate();
                creator = metadata.getCreator();
                keywords = metadata.getKeywords();
                modificationDate = metadata.getModificationDate();
                producer = metadata.getProducer();
                subject = metadata.getSubject();
                title = metadata.getTitle();

                document.close();
            }
            return true;
        } catch (IOException | IllegalArgumentException e) {
            // problem with source document
        } catch ( PrinterException e) {
            // permissions prevent printing
        }
        return false;
    }

    public Integer getPageCount() {
        return pageCount;
    }

    public String getAuthor() {
        return author;
    }

    public Calendar getCreationDate() {
        return creationDate;
    }

    public String getCreator() {
        return creator;
    }

    public String getKeywords() {
        return keywords;
    }

    public Calendar getModificationDate() {
        return modificationDate;
    }

    public String getProducer() {
        return producer;
    }

    public String getSubject() {
        return subject;
    }

    public String getTitle() {
        return title;
    }

    public String getOrientation() {
        return orientation;
    }

    public Boolean isEncrypted() {
        return encrypted;
    }

    public String getVersion() {
        return version;
    }

    /**
     * Set the input stream to the argument stream (or file).
     *
     * @param inputStream the input stream to read from
     */
    public void setInput( InputStream inputStream ) {
        in = inputStream;
    }
}
