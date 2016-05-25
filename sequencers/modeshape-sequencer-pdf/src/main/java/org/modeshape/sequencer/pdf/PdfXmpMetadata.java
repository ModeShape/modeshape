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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.common.PDMetadata;
import org.apache.xmpbox.XMPMetadata;
import org.apache.xmpbox.schema.XMPBasicSchema;
import org.apache.xmpbox.xml.DomXmpParser;

/**
 * Utility for extracting XMP metadata from PDF files.
 * 
 * @since 5.1
 */
public class PdfXmpMetadata {

    private String baseURL;
    private Calendar createDate;
    private String creatorTool;
    private List<String> identifier = new ArrayList<>();
    private Calendar metadataDate;
    private Calendar modifyDate;
    private String nickname;
    private Integer rating;
    private String label;

    private InputStream in;

    public PdfXmpMetadata( InputStream inputStream ) {
        this.in = inputStream;
    }

    public boolean check() throws Exception {
        try (PDDocument document = PDDocument.load(in)) {
            Boolean encrypted = document.isEncrypted();

            if (encrypted) {
                return false;
            }

            PDDocumentCatalog catalog = document.getDocumentCatalog();
            PDMetadata metadata = catalog.getMetadata();
            if (metadata == null) {
                return false;
            }

            DomXmpParser xmpParser = new DomXmpParser();
            try (InputStream is = metadata.createInputStream()) {
                XMPMetadata xmp = xmpParser.parse(is);
                XMPBasicSchema basicSchema = xmp.getXMPBasicSchema();
                if (basicSchema != null) {
                    baseURL = basicSchema.getBaseURL();
                    createDate = basicSchema.getCreateDate();
                    creatorTool = basicSchema.getCreatorTool();
                    if (basicSchema.getIdentifiers() != null) {
                        identifier.addAll(basicSchema.getIdentifiers());
                    }
                    metadataDate = basicSchema.getMetadataDate();
                    modifyDate = basicSchema.getModifyDate();
                    nickname = basicSchema.getNickname();
                    rating = basicSchema.getRating();
                    label = basicSchema.getLabel();
                    return true;
                }
                return false;
            }
            
        }
    }


    public String getBaseURL() {
        return baseURL;
    }

    public Calendar getCreateDate() {
        return createDate;
    }

    public String getCreatorTool() {
        return creatorTool;
    }

    public List<String> getIdentifier() {
        return identifier;
    }

    public Calendar getMetadataDate() {
        return metadataDate;
    }

    public Calendar getModifyDate() {
        return modifyDate;
    }

    public String getNickname() {
        return nickname;
    }

    public Integer getRating() {
        return rating;
    }

    public String getLabel() {
        return label;
    }

}