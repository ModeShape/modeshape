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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.apache.jempbox.impl.XMLUtil;
import org.apache.jempbox.xmp.XMPMetadata;
import org.apache.jempbox.xmp.XMPSchemaBasic;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.common.PDMetadata;

/**
 * Utility for extracting XMP metadata from PDF files.
 */
public class PdfXmpMetadata {

    private String baseURL;
    private Calendar createDate;
    private String creatorTool;
    private List<String> identifier = new ArrayList<String>();
    private String label;
    private Calendar metadataDate;
    private Calendar modifyDate;
    private String nickname;
    private Integer rating;

    private InputStream in;

    public boolean check() {
        try {
            PDDocument document = PDDocument.load(in);
            Boolean encrypted = document.isEncrypted();

            if (encrypted) {
                return false;
            }

            PDDocumentCatalog catalog = document.getDocumentCatalog();
            PDMetadata metadata = catalog.getMetadata();
            if (metadata == null) {
                return false;
            }

            // when pdfbox-2.0.0 is released, replace with org.apache.pdfbox:xmpbox library
            XMPMetadata xmpMetadata = new XMPMetadata(XMLUtil.parse(metadata.createInputStream()));
            XMPSchemaBasic xmpSchema = xmpMetadata.getBasicSchema();
            if (xmpSchema != null) {
                baseURL = xmpSchema.getBaseURL();
                createDate = xmpSchema.getCreateDate();
                creatorTool = xmpSchema.getCreatorTool();
                if (xmpSchema.getIdentifiers() != null) {
                    identifier.addAll(xmpSchema.getIdentifiers());
                }
                label = xmpSchema.getLabel();
                metadataDate = xmpSchema.getMetadataDate();
                modifyDate = xmpSchema.getModifyDate();
                nickname = xmpSchema.getNickname();
                rating = xmpSchema.getRating();
            }
            return true;
        } catch (IOException | IllegalArgumentException e) {
            // problem with source document
        }
        return false;
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

    public String getLabel() {
        return label;
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

    /**
     * Set the input stream to the argument stream (or file).
     *
     * @param inputStream the input stream to read from
     */
    public void setInput( InputStream inputStream ) {
        in = inputStream;
    }
}