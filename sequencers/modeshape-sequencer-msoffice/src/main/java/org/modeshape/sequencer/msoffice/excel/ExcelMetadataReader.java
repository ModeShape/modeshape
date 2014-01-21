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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFComment;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;

/**
 * Extracts data and some metadata from Excel files
 */
public class ExcelMetadataReader {

    /** The character to output after each row. */
    private static final char ROW_DELIMITER_CHAR = '\n';
    /** The character to output after each cell (column). */
    private static final char CELL_DELIMITER_CHAR = '\t';

    public static ExcelMetadata instance( InputStream stream ) throws IOException {
        ExcelMetadata metadata = new ExcelMetadata();
        HSSFWorkbook wb = new HSSFWorkbook(new POIFSFileSystem(stream));

        List<ExcelSheetMetadata> sheets = new ArrayList<ExcelSheetMetadata>();

        for (int sheetInd = 0; sheetInd < wb.getNumberOfSheets(); sheetInd++) {
            ExcelSheetMetadata meta = new ExcelSheetMetadata();
            meta.setName(wb.getSheetName(sheetInd));
            sheets.add(meta);

            HSSFSheet worksheet = wb.getSheetAt(sheetInd);
            int lastRowNum = worksheet.getLastRowNum();

            StringBuilder buff = new StringBuilder();
            for (int rowNum = worksheet.getFirstRowNum(); rowNum <= lastRowNum; rowNum++) {
                HSSFRow row = worksheet.getRow(rowNum);

                // Empty rows are returned as null
                if (row == null) {
                    continue;
                }

                int lastCellNum = row.getLastCellNum();
                for (int cellNum = row.getFirstCellNum(); cellNum < lastCellNum; cellNum++) {
                    HSSFCell cell = row.getCell(cellNum);

                    // Undefined cells are returned as null
                    if (cell == null) {
                        continue;
                    }

                    /*
                     * Builds a string of body content from all string, numeric,
                     * and formula values in the body of each worksheet.
                     * 
                     *  This code currently duplicates the POI 3.1 ExcelExtractor behavior of
                     *  combining the body text from all worksheets into a single string.
                     */
                    switch (cell.getCellType()) {
                        case HSSFCell.CELL_TYPE_STRING:
                            buff.append(cell.getRichStringCellValue().getString());
                            break;
                        case HSSFCell.CELL_TYPE_NUMERIC:
                            buff.append(cell.getNumericCellValue());
                            break;
                        case HSSFCell.CELL_TYPE_FORMULA:
                            buff.append(cell.getCellFormula());
                            break;
                    }

                    HSSFComment comment = cell.getCellComment();
                    if (comment != null) {
                        // Filter out row delimiter characters from comment
                        String commentText = comment.getString().getString().replace(ROW_DELIMITER_CHAR, ' ');

                        buff.append(" [");
                        buff.append(commentText);
                        buff.append(" by ");
                        buff.append(comment.getAuthor());
                        buff.append(']');
                    }

                    if (cellNum < lastCellNum - 1) {
                        buff.append(CELL_DELIMITER_CHAR);
                    } else {
                        buff.append(ROW_DELIMITER_CHAR);
                    }
                }
            }
            meta.setText(buff.toString());
        }

        metadata.setSheets(sheets);
        metadata.setMetadata(wb.getSummaryInformation());
        return metadata;
    }
}
