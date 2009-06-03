/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * JBoss DNA is free software. Unless otherwise indicated, all code in JBoss DNA
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * JBoss DNA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.repository.config;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import org.jboss.dna.graph.connector.RepositorySource;
import org.xml.sax.SAXException;

/**
 * 
 */
public class JcrConfiguration extends DnaConfiguration {

    /**
     * {@inheritDoc}
     * 
     * @throws IOException
     * @throws SAXException
     * @see org.jboss.dna.repository.config.DnaConfiguration#loadFrom(java.lang.String)
     */
    @Override
    public JcrConfiguration loadFrom( String pathToFile ) throws IOException, SAXException {
        super.loadFrom(pathToFile);
        return this;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.repository.config.DnaConfiguration#loadFrom(java.io.File)
     */
    @Override
    public JcrConfiguration loadFrom( File configurationFile ) throws IOException, SAXException {
        super.loadFrom(configurationFile);
        return this;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.repository.config.DnaConfiguration#loadFrom(java.net.URL)
     */
    @Override
    public JcrConfiguration loadFrom( URL urlToConfigurationFile ) throws IOException, SAXException {
        super.loadFrom(urlToConfigurationFile);
        return this;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.repository.config.DnaConfiguration#loadFrom(java.io.InputStream)
     */
    @Override
    public JcrConfiguration loadFrom( InputStream configurationFileInputStream ) throws IOException, SAXException {
        super.loadFrom(configurationFileInputStream);
        return this;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.repository.config.DnaConfiguration#loadFrom(org.jboss.dna.graph.connector.RepositorySource)
     */
    @Override
    public JcrConfiguration loadFrom( RepositorySource source ) {
        super.loadFrom(source);
        return this;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.repository.config.DnaConfiguration#loadFrom(org.jboss.dna.graph.connector.RepositorySource,
     *      java.lang.String)
     */
    @Override
    public JcrConfiguration loadFrom( RepositorySource source,
                                      String workspaceName ) {
        super.loadFrom(source, workspaceName);
        return this;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.repository.config.DnaConfiguration#loadFrom(org.jboss.dna.graph.connector.RepositorySource,
     *      java.lang.String, java.lang.String)
     */
    @Override
    public JcrConfiguration loadFrom( RepositorySource source,
                                      String workspaceName,
                                      String pathInWorkspace ) {
        super.loadFrom(source, workspaceName, pathInWorkspace);
        return this;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.repository.config.DnaConfiguration#mimeTypeDetector(java.lang.String)
     */
    @Override
    public MimeTypeDetectorDetails<JcrConfiguration> mimeTypeDetector( String name ) {
        return new MimeTypeDetectorBuilder<JcrConfiguration>(this);
    }

    public JcrConfiguration addRepository( String repositoryId ) {
        return this;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.repository.config.DnaConfiguration#build()
     */
    @Override
    public JcrEngine build() {
        return new JcrEngine();
    }
}
