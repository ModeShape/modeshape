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

/**
 * @author Randall Hauch
 */
public class MockUserInterface implements UserInterface {

    private final String repositoryPath;
    private final URL fileToUpload;
    private final int numberOfSearchResults;

    public MockUserInterface( URL fileToUpload,
                              String repositoryPath,
                              int numSearchResults ) {
        this.repositoryPath = repositoryPath;
        this.fileToUpload = fileToUpload;
        this.numberOfSearchResults = numSearchResults;
    }

    /**
     * {@inheritDoc}
     */
    public void displaySearchResults( List<MediaInfo> images ) {
        assertThat(images.size(), is(this.numberOfSearchResults));
        for (MediaInfo image : images) {
            System.out.println("Image: " + image);
        }
    }

    /**
     * {@inheritDoc}
     */
    public URL getFileToUpload() {
        return this.fileToUpload;
    }

    /**
     * {@inheritDoc}
     */
    public String getRepositoryPath( String defaultPath ) {
        return this.repositoryPath != null ? this.repositoryPath : defaultPath;
    }

}
