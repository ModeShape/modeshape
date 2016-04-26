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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.poi.hslf.usermodel.HSLFSlide;
import org.apache.poi.hslf.usermodel.HSLFSlideShow;
import org.apache.poi.hslf.usermodel.HSLFSlideShowImpl;
import org.apache.poi.hslf.usermodel.HSLFTextParagraph;
import org.apache.poi.hslf.usermodel.HSLFTextRun;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;

/**
 * Utility for extracting metadata from PowerPoint files
 */
public class PowerPointMetadataReader {

    public static PowerpointMetadata instance( InputStream stream ) throws IOException {
        POIFSFileSystem fs = new POIFSFileSystem(stream);
        HSLFSlideShow rawSlideShow = new HSLFSlideShow(fs);
        List<SlideMetadata> slidesMetadata = rawSlideShow.getSlides()
                                                         .stream()
                                                         .map(slide -> processSlide(rawSlideShow, slide))
                                                         .collect(Collectors.toList());

        PowerpointMetadata deck = new PowerpointMetadata();
        deck.setSlides(slidesMetadata);
        deck.setMetadata(new HSLFSlideShowImpl(fs).getSummaryInformation());
        return deck;
    }

    private static SlideMetadata processSlide(HSLFSlideShow rawSlideShow, HSLFSlide slide) {
        SlideMetadata slideMetadata = new SlideMetadata();
        // process title
        String title = slide.getTitle();
        slideMetadata.setTitle(title);

        // process notes
        slideMetadata.setNotes(collectText(slide.getNotes().getTextParagraphs(), title));
        // process text
        slideMetadata.setText(collectText(slide.getTextParagraphs(), title));

        // process thumbnail
        Dimension pgsize = rawSlideShow.getPageSize();

        BufferedImage img = new BufferedImage(pgsize.width, pgsize.height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = img.createGraphics();
        // clear the drawing area
        graphics.setPaint(Color.white);
        graphics.fill(new Rectangle2D.Float(0, 0, pgsize.width, pgsize.height));

        // render
        slide.draw(graphics);

        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            javax.imageio.ImageIO.write(img, "png", out);
            slideMetadata.setThumbnail(out.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    
        return slideMetadata;
    }

    private static String collectText(List<List<HSLFTextParagraph>> paragraphs, String title) {
        return paragraphs.stream()
                         .flatMap(Collection::stream)
                         .flatMap(paragraph -> paragraph.getTextRuns().stream())
                         .map(HSLFTextRun::getRawText)
                         .filter(rawText -> !title.equals(rawText))
                         .collect(Collectors.joining());
    }
}
