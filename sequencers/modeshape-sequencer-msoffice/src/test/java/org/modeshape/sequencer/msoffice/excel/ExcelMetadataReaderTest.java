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

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import org.junit.Test;
import java.io.InputStream;

/**
 * Unit test for {@link ExcelMetadataReader}
 * 
 * @author Michael Trezzi
 * @author Horia Chiorean
 */
public class ExcelMetadataReaderTest {

    @Test
    public void shouldBeAbleToCreateMetadataForExcel() throws Exception {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("excel.xls");

        ExcelMetadata excelMetadata = ExcelMetadataReader.instance(inputStream);
        ExcelSheetMetadata sheet2 = excelMetadata.getSheet("MySheet2");
        assertThat(sheet2, is(notNullValue()));
        assertThat(excelMetadata.getSheet("Sheet1").getText().startsWith("This is a text"), is(true));
    }
}
