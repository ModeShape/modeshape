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

package org.modeshape.sequencer.msoffice.powerpoint;

import java.util.Collections;
import java.util.List;
import org.apache.poi.hpsf.SummaryInformation;
import org.modeshape.sequencer.msoffice.MSOfficeMetadata;

/**
 * Metadata for Microsoft Powerpoint decks.
 */
public class PowerpointMetadata {

    private List<SlideMetadata> slides = Collections.emptyList();
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

    public List<SlideMetadata> getSlides() {
        return slides;
    }

    public void setSlides( List<SlideMetadata> slides ) {
        this.slides = slides;
    }
}
