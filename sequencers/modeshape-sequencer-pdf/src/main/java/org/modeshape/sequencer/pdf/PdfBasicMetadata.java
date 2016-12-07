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
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.filespecification.PDComplexFileSpecification;
import org.apache.pdfbox.pdmodel.common.filespecification.PDEmbeddedFile;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationFileAttachment;
import org.apache.pdfbox.printing.PDFPageable;

/**
 * Utility for extracting Document Information Directory metadata from PDF files.
 * 
 * @since 5.1
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

    private List<PdfPageMetadata> pages = new ArrayList<>();

    private InputStream in;

    public PdfBasicMetadata( InputStream inputStream ) {
        this.in = inputStream;
    }

    /*
     * Check that given file is supported by this sequencer.
     */
    public boolean check() throws Exception {
        try (PDDocument document = PDDocument.load(in)) {
            PDDocumentCatalog catalog = document.getDocumentCatalog();
            PDFPageable pageable = new PDFPageable(document);
            PageFormat firstPage = pageable.getPageFormat(0);

            encrypted = document.isEncrypted();
            pageCount = document.getNumberOfPages();
            orientation = ORIENTATION_STRINGS[firstPage.getOrientation()];
            version = String.valueOf(document.getDocument().getVersion());
            String catalogVersion = catalog.getVersion();
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
            }

            // extract all attached files from all pages
            int pageNumber = 0;
            for (Object page : catalog.getPages()) {
                pageNumber += 1;
                PdfPageMetadata pageMetadata = new PdfPageMetadata();
                pageMetadata.setPageNumber(pageNumber);
                for (PDAnnotation annotation : ((PDPage) page).getAnnotations()) {
                    if (annotation instanceof PDAnnotationFileAttachment) {
                        PdfAttachmentMetadata attachmentMetadata = new PdfAttachmentMetadata();

                        PDAnnotationFileAttachment fann = (PDAnnotationFileAttachment) annotation;
                        PDComplexFileSpecification fileSpec = (PDComplexFileSpecification) fann.getFile();
                        PDEmbeddedFile embeddedFile = fileSpec.getEmbeddedFile();

                        attachmentMetadata.setSubject(fann.getSubject());
                        attachmentMetadata.setName(fileSpec.getFilename());
                        attachmentMetadata.setCreationDate(embeddedFile.getCreationDate());
                        attachmentMetadata.setModificationDate(embeddedFile.getModDate());
                        attachmentMetadata.setMimeType(embeddedFile.getSubtype());
                        attachmentMetadata.setData(embeddedFile.toByteArray());

                        pageMetadata.addAttachment(attachmentMetadata);
                    }
                }
                pages.add(pageMetadata);
            }
            return true;
        }
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

    public boolean isEncrypted() {
        return encrypted;
    }

    public String getVersion() {
        return version;
    }

    public List<PdfPageMetadata> getPages() {
        return pages;
    }

    public void addPage( PdfPageMetadata page ) {
        this.pages.add(page);
    }

}
