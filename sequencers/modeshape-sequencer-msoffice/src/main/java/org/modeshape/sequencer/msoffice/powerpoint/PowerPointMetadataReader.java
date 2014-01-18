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
import java.util.ArrayList;
import java.util.List;
import org.apache.poi.hslf.HSLFSlideShow;
import org.apache.poi.hslf.model.Slide;
import org.apache.poi.hslf.model.TextRun;
import org.apache.poi.hslf.usermodel.SlideShow;

/**
 * Utility for extracting metadata from PowerPoint files
 */
public class PowerPointMetadataReader {

    public static PowerpointMetadata instance( InputStream stream ) throws IOException {
        HSLFSlideShow rawSlideShow = new HSLFSlideShow(stream);
        SlideShow slideshow = new SlideShow(rawSlideShow);
        Slide[] slides = slideshow.getSlides();

        List<SlideMetadata> slidesMetadata = new ArrayList<SlideMetadata>();

        for (Slide slide : slides) {
            SlideMetadata slideMetadata = new SlideMetadata();
            // process title
            slideMetadata.setTitle(slide.getTitle());

            // process notes
            for (TextRun textRun : slide.getNotesSheet().getTextRuns()) {
                if (slideMetadata.getNotes() == null) {
                    slideMetadata.setNotes("");
                }
                slideMetadata.setNotes(slideMetadata.getNotes() + textRun.getText());
            }
            // process text
            for (TextRun textRun : slide.getTextRuns()) {
                if (!textRun.getText().equals(slideMetadata.getTitle()) && textRun.getText() != null) {
                    if (slideMetadata.getText() == null) {
                        slideMetadata.setText("");
                    }
                    slideMetadata.setText(slideMetadata.getText() + textRun.getText());
                }
            }

            // process thumbnail
            Dimension pgsize = slideshow.getPageSize();

            BufferedImage img = new BufferedImage(pgsize.width, pgsize.height, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = img.createGraphics();
            // clear the drawing area
            graphics.setPaint(Color.white);
            graphics.fill(new Rectangle2D.Float(0, 0, pgsize.width, pgsize.height));

            // render
            slide.draw(graphics);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            javax.imageio.ImageIO.write(img, "png", out);
            slideMetadata.setThumbnail(out.toByteArray());

            slidesMetadata.add(slideMetadata);

        }

        PowerpointMetadata deck = new PowerpointMetadata();
        deck.setSlides(slidesMetadata);
        deck.setMetadata(rawSlideShow.getSummaryInformation());
        return deck;
    }

}
