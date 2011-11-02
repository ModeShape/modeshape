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
package org.modeshape.jcr.core.text;

import java.io.IOException;
import java.io.InputStream;
import org.modeshape.common.annotation.ThreadSafe;
import org.modeshape.common.component.ClassLoaderFactory;
import org.modeshape.common.component.ComponentLibrary;
import org.modeshape.common.component.StandardClassLoaderFactory;
import org.modeshape.common.util.Logger;

/**
 * Facility for managing {@link TextExtractorConfig}s.
 */
@ThreadSafe
public final class TextExtractors implements TextExtractor {

    public static final String DEFAULT_MIME_TYPE = "application/octet-stream";

    /**
     * Class loader factory instance that always returns the {@link Thread#getContextClassLoader() current thread's context class
     * loader}, or if <code>null</code> the class loader for this class.
     */
    protected static final ClassLoaderFactory DEFAULT_CLASSLOADER_FACTORY = new StandardClassLoaderFactory(
                                                                                                           TextExtractors.class.getClassLoader());

    private final ComponentLibrary<TextExtractor, TextExtractorConfig> library;
    private Logger logger;
    private final boolean stopAfterFirst;

    public TextExtractors() {
        library = new ComponentLibrary<TextExtractor, TextExtractorConfig>(true);
        library.setClassLoaderFactory(DEFAULT_CLASSLOADER_FACTORY);
        stopAfterFirst = true;
    }

    /**
     * @return library
     */
    public ComponentLibrary<TextExtractor, TextExtractorConfig> getLibrary() {
        return library;
    }

    /**
     * Get the number of text extractors.
     * 
     * @return the number of text extractors; may be 0 or greater
     */
    public int size() {
        return library.getSequenceConfigs().size();
    }

    /**
     * Adds the configuration for a text extractor <em>before</em> any previously added configurations, or updates any existing
     * one that represents the {@link TextExtractorConfig#equals(Object) same configuration}
     * 
     * @param config the new configuration; must not be <code>null</code>.
     * @return <code>true</code> if the detector was added, or <code>false</code> if there already was an existing detector
     *         configuration.
     * @see #removeExtractor(TextExtractorConfig)
     */
    public boolean addExtractor( TextExtractorConfig config ) {
        return library.add(config);
    }

    /**
     * Gets the class loader factory that should be used to load text extractors. By default, this service uses a factory that
     * will return either the {@link Thread#getContextClassLoader() current thread's context class loader}, or if
     * <code>null</code> the class loader for this class.
     * 
     * @return the class loader factory; never <code>null</code>
     * @see #setClassLoaderFactory(ClassLoaderFactory)
     */
    public ClassLoaderFactory getClassLoaderFactory() {
        return library.getClassLoaderFactory();
    }

    /**
     * Gets the logger for this system
     * 
     * @return the logger
     */
    public synchronized Logger getLogger() {
        if (logger == null) {
            logger = Logger.getLogger(getClass());
        }
        return logger;
    }

    @Override
    public boolean supportsMimeType( String mimeType ) {
        for (TextExtractor extractor : library.getInstances()) {
            if (extractor.supportsMimeType(mimeType)) return true;
        }
        return false;
    }

    @Override
    public void extractFrom( InputStream stream,
                             TextExtractorOutput output,
                             TextExtractorContext context ) throws IOException {
        if (stream == null) return;
        if (stream.markSupported()) {
            stream.mark(Integer.MAX_VALUE);
        }
        final String mimeType = context.getMimeType();

        // Run through the extractors and have them extract the text
        for (TextExtractor extractor : library.getInstances()) {
            if (!extractor.supportsMimeType(mimeType)) continue;
            extractor.extractFrom(stream, output, context);
            if (stopAfterFirst || !stream.markSupported()) break;
            stream.reset();
        }
    }

    /**
     * Removes the configuration for a text extractor.
     * 
     * @param config the configuration to be removed; must not be <code>null</code>.
     * @return <code>true</code> if the configuration was removed, or <code>false</code> if there was no existing configuration
     * @see #addExtractor(TextExtractorConfig)
     */
    public boolean removeExtractor( TextExtractorConfig config ) {
        return library.remove(config);
    }

    /**
     * Sets the Maven Repository that should be used to load the MIME-type detectors. By default, this service uses a factory that
     * will return either the {@link Thread#getContextClassLoader() current thread's context class loader}, or if
     * <code>null</code> the class loader for this class.
     * 
     * @param classLoaderFactory the class loader factory, or <code>null</code> if the default class loader factory should be
     *        used.
     * @see #getClassLoaderFactory()
     */
    public void setClassLoaderFactory( ClassLoaderFactory classLoaderFactory ) {
        library.setClassLoaderFactory(classLoaderFactory != null ? classLoaderFactory : DEFAULT_CLASSLOADER_FACTORY);
    }

    /**
     * Sets the logger for this system.
     * 
     * @param logger the logger, or <code>null</code> if the standard logging should be used
     */
    public synchronized void setLogger( Logger logger ) {
        this.logger = logger != null ? logger : getLogger();
    }
}
