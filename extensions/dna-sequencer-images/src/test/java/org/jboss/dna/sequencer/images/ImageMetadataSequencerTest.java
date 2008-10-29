/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.sequencer.images;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.number.IsCloseTo.closeTo;
import static org.junit.Assert.assertThat;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import org.jboss.dna.common.i18n.MockI18n;
import org.jboss.dna.common.monitor.ActivityMonitor;
import org.jboss.dna.common.monitor.SimpleActivityMonitor;
import org.jboss.dna.graph.sequencers.MockSequencerContext;
import org.jboss.dna.graph.sequencers.MockSequencerOutput;
import org.jboss.dna.graph.sequencers.SequencerContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Randall Hauch
 * @author John Verhaeg
 */
public class ImageMetadataSequencerTest {

    private ImageMetadataSequencer sequencer;
    private InputStream content;
    private MockSequencerOutput output;
    private ActivityMonitor activityMonitor;
    private URL cautionGif;
    private URL cautionJpg;
    private URL cautionPict;
    private URL cautionPng;
    private SequencerContext context;

    @Before
    public void beforeEach() {
        sequencer = new ImageMetadataSequencer();
        context = new MockSequencerContext();
        context.getNamespaceRegistry().register("image", "http://jboss.org/dna/images/1.0");
        output = new MockSequencerOutput(context);
        activityMonitor = new SimpleActivityMonitor(MockI18n.passthrough, "Test Activity");
        cautionGif = this.getClass().getClassLoader().getResource("caution.gif");
        cautionJpg = this.getClass().getClassLoader().getResource("caution.jpg");
        cautionPict = this.getClass().getClassLoader().getResource("caution.pict");
        cautionPng = this.getClass().getClassLoader().getResource("caution.png");
    }

    @After
    public void afterEach() throws Exception {
        if (content != null) {
            try {
                content.close();
            } finally {
                content = null;
            }
        }
    }

    @Test
    public void shouldGenerateMetadataForJpegImageFiles() throws IOException {
        URL url = this.cautionJpg;
        assertThat(url, is(notNullValue()));
        content = url.openStream();
        assertThat(content, is(notNullValue()));
        sequencer.sequence(content, output, context, activityMonitor);
        assertThat(output.getPropertyValues("image:metadata", "jcr:primaryType"), is(new Object[] {"image:metadata"}));
        assertThat(output.getPropertyValues("image:metadata", "jcr:mimeType"), is(new Object[] {"image/jpeg"}));
        assertThat(output.getPropertyValues("image:metadata", "image:formatName"), is(new Object[] {"JPEG"}));
        assertThat(output.getPropertyValues("image:metadata", "image:width"), is(new Object[] {48}));
        assertThat(output.getPropertyValues("image:metadata", "image:height"), is(new Object[] {48}));
        assertThat(output.getPropertyValues("image:metadata", "image:bitsPerPixel"), is(new Object[] {24}));
        assertThat(output.getPropertyValues("image:metadata", "image:progressive"), is(new Object[] {false}));
        assertThat(output.getPropertyValues("image:metadata", "image:numberOfImages"), is(new Object[] {1}));
        assertThat(output.getPropertyValues("image:metadata", "image:physicalWidthDpi"), is(new Object[] {72}));
        assertThat(output.getPropertyValues("image:metadata", "image:physicalHeightDpi"), is(new Object[] {72}));
        assertThat(((Float)(output.getPropertyValues("image:metadata", "image:physicalWidthInches")[0])).doubleValue(),
                   is(closeTo(0.666667d, 0.0001d)));
        assertThat(((Float)(output.getPropertyValues("image:metadata", "image:physicalHeightInches")[0])).doubleValue(),
                   is(closeTo(0.666667d, 0.0001d)));
    }

    @Test
    public void shouldGenerateMetadataForPngImageFiles() throws IOException {
        URL url = this.cautionPng;
        assertThat(url, is(notNullValue()));
        content = url.openStream();
        assertThat(content, is(notNullValue()));
        sequencer.sequence(content, output, context, activityMonitor);
        assertThat(output.getPropertyValues("image:metadata", "jcr:primaryType"), is(new Object[] {"image:metadata"}));
        assertThat(output.getPropertyValues("image:metadata", "jcr:mimeType"), is(new Object[] {"image/png"}));
        assertThat(output.getPropertyValues("image:metadata", "image:formatName"), is(new Object[] {"PNG"}));
        assertThat(output.getPropertyValues("image:metadata", "image:width"), is(new Object[] {48}));
        assertThat(output.getPropertyValues("image:metadata", "image:height"), is(new Object[] {48}));
        assertThat(output.getPropertyValues("image:metadata", "image:bitsPerPixel"), is(new Object[] {24}));
        assertThat(output.getPropertyValues("image:metadata", "image:progressive"), is(new Object[] {false}));
        assertThat(output.getPropertyValues("image:metadata", "image:numberOfImages"), is(new Object[] {1}));
        assertThat(output.getPropertyValues("image:metadata", "image:physicalWidthDpi"), is(new Object[] {-1}));
        assertThat(output.getPropertyValues("image:metadata", "image:physicalHeightDpi"), is(new Object[] {-1}));
        assertThat(((Float)(output.getPropertyValues("image:metadata", "image:physicalWidthInches")[0])), is(-1f));
        assertThat(((Float)(output.getPropertyValues("image:metadata", "image:physicalHeightInches")[0])), is(-1f));
    }

    @Test
    public void shouldGenerateMetadataForGifImageFiles() throws IOException {
        URL url = this.cautionGif;
        assertThat(url, is(notNullValue()));
        content = url.openStream();
        assertThat(content, is(notNullValue()));
        sequencer.sequence(content, output, context, activityMonitor);
        assertThat(output.getPropertyValues("image:metadata", "jcr:mimeType"), is(new Object[] {"image/gif"}));
        assertThat(output.getPropertyValues("image:metadata", "image:formatName"), is(new Object[] {"GIF"}));
        assertThat(output.getPropertyValues("image:metadata", "image:width"), is(new Object[] {48}));
        assertThat(output.getPropertyValues("image:metadata", "image:height"), is(new Object[] {48}));
        assertThat(output.getPropertyValues("image:metadata", "image:bitsPerPixel"), is(new Object[] {8}));
        assertThat(output.getPropertyValues("image:metadata", "image:progressive"), is(new Object[] {false}));
        assertThat(output.getPropertyValues("image:metadata", "image:numberOfImages"), is(new Object[] {1}));
        assertThat(output.getPropertyValues("image:metadata", "image:physicalWidthDpi"), is(new Object[] {-1}));
        assertThat(output.getPropertyValues("image:metadata", "image:physicalHeightDpi"), is(new Object[] {-1}));
        assertThat(((Float)(output.getPropertyValues("image:metadata", "image:physicalWidthInches")[0])), is(-1f));
        assertThat(((Float)(output.getPropertyValues("image:metadata", "image:physicalHeightInches")[0])), is(-1f));
    }

    @Test
    public void shouldGenerateNoMetadataforPictImageFiles() throws IOException {
        URL url = this.cautionPict;
        assertThat(url, is(notNullValue()));
        content = url.openStream();
        assertThat(content, is(notNullValue()));
        sequencer.sequence(content, output, context, activityMonitor);
        assertThat(output.hasProperties(), is(false));

    }
}
