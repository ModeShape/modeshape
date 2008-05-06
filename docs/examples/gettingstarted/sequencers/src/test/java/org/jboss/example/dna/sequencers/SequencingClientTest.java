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
package org.jboss.example.dna.sequencers;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import java.net.URL;
import java.util.List;
import org.jboss.dna.common.util.FileUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Randall Hauch
 */
public class SequencingClientTest {

    private URL file;
    private SequencingClient client;

    @Before
    public void beforeEach() throws Exception {
        this.file = Thread.currentThread().getContextClassLoader().getResource("caution.png");
        client = new SequencingClient();
        client.setWorkingDirectory("target/repositoryData");
        client.setJackrabbitConfigPath("src/main/resources/jackrabbitConfig.xml");
        FileUtil.delete("target/repositoryData");
    }

    @After
    public void afterEach() throws Exception {
        client.shutdownDnaServices();
        client.shutdownRepository();
        FileUtil.delete("target/repositoryData");
    }

    @Test
    public void shouldStartupAndShutdownRepository() throws Exception {
        client.startRepository();
        client.shutdownRepository();

    }

    @Test
    public void shouldStartupAndShutdownRepositoryAndSequencingService() throws Exception {
        client.startRepository();
        client.startDnaServices();
        client.shutdownDnaServices();
        client.shutdownRepository();
    }

    @Test
    public void shouldUploadFile() throws Exception {
        client.startRepository();
        client.startDnaServices();
        client.uploadFile(file.openStream(), "/a/b/caution.png", "image/png");

        // Let the sequencing start ...
        Thread.sleep(1000);
        client.shutdownDnaServices(); // this will block untill all processing has been done ...

        List<ImageInfo> images = client.getImages();
        assertThat(images.size(), is(1));
        for (ImageInfo image : images) {
            System.out.println("Image: " + image);
        }
    }

}
