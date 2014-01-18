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
package org.modeshape.sequencer.msoffice.excel;

import java.util.List;
import org.apache.poi.hpsf.SummaryInformation;
import org.modeshape.sequencer.msoffice.MSOfficeMetadata;

/**
 * Metadata for Excel (Full text contents and list of sheet names)
 */
public class ExcelMetadata {

    private String text;
    private List<ExcelSheetMetadata> sheets;
    private MSOfficeMetadata metadata;

    public MSOfficeMetadata getMetadata() {
        return metadata;
    }

    public void setMetadata( MSOfficeMetadata metadata ) {
        this.metadata = metadata;
    }

    public void setMetadata( SummaryInformation info ) {
        if (info != null) {
            metadata = new MSOfficeMetadata();
            metadata.setSummaryInformation(info);
        }
    }

    public String getText() {
        return text;
    }

    public void setText( String text ) {
        this.text = text;
    }

    public List<ExcelSheetMetadata> getSheets() {
        return sheets;
    }

    public void setSheets( List<ExcelSheetMetadata> sheets ) {
        this.sheets = sheets;
    }

    public ExcelSheetMetadata getSheet( String name ) {
        for (ExcelSheetMetadata sheet : sheets) {
            if (sheet.getName().equals(name)) return sheet;
        }
        return null;
    }
}
