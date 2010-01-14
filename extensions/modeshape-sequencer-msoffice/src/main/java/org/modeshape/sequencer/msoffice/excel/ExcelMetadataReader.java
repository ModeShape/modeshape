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

        StringBuffer buff = new StringBuffer();
        List<String> sheets = new ArrayList<String>();

        for (int sheetInd = 0; sheetInd < wb.getNumberOfSheets(); sheetInd++) {
            sheets.add(wb.getSheetName(sheetInd));

            HSSFSheet worksheet = wb.getSheetAt(sheetInd);
            int lastRowNum = worksheet.getLastRowNum();

            for (int rowNum = worksheet.getFirstRowNum(); rowNum <= lastRowNum; rowNum++) {
                HSSFRow row = worksheet.getRow(rowNum);

                // Empty rows are returned as null
                if (row == null) {
                    continue;
                }

                int lastCellNum = row.getLastCellNum();
                for (int cellNum = row.getFirstCellNum(); cellNum < lastCellNum; cellNum++) {
                    HSSFCell cell = row.getCell(cellNum);

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
        }

        metadata.setText(buff.toString());
        metadata.setSheets(sheets);
        return metadata;
    }
}
