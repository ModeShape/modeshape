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
package org.modeshape.sequencer.msoffice;

import org.apache.poi.poifs.eventfilesystem.POIFSReader;
import java.io.InputStream;

/**
 * Utility for extracting metadata from Excel files
 */
public class MSOfficeMetadataReader {

    public static MSOfficeMetadata instance( InputStream stream ) throws Exception {
        POIFSReader r = new POIFSReader();
        MSOfficeMetadata MSOfficeMetadataListener = new MSOfficeMetadata();
        r.registerListener(MSOfficeMetadataListener, "\005SummaryInformation");
        r.read(stream);
        return MSOfficeMetadataListener;
    }
}
