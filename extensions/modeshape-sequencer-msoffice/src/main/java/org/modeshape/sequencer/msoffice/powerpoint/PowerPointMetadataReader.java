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
import org.apache.poi.hslf.model.Slide;
import org.apache.poi.hslf.model.TextRun;
import org.apache.poi.hslf.usermodel.SlideShow;

/**
 * Utility for extracting metadata from PowerPoint files
 */
public class PowerPointMetadataReader {

    public static List<SlideMetadata> instance( InputStream stream ) throws IOException {
        SlideShow slideshow = new SlideShow(stream);
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

        return slidesMetadata;
    }

}
