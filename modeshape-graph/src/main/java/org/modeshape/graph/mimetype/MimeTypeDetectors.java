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
package org.modeshape.graph.mimetype;

import java.io.IOException;
import java.io.InputStream;
import net.jcip.annotations.ThreadSafe;
import org.modeshape.common.component.ClassLoaderFactory;
import org.modeshape.common.component.ComponentLibrary;
import org.modeshape.common.component.StandardClassLoaderFactory;
import org.modeshape.common.util.Logger;

/**
 * Facility for managing {@link MimeTypeDetectorConfig}s.
 */
@ThreadSafe
public final class MimeTypeDetectors implements MimeTypeDetector {

    public static final String DEFAULT_MIME_TYPE = "application/octet-stream";

    /**
     * Class loader factory instance that always returns the {@link Thread#getContextClassLoader() current thread's context class
     * loader}, or if <code>null</code> the class loader for this class.
     */
    protected static final ClassLoaderFactory DEFAULT_CLASSLOADER_FACTORY = new StandardClassLoaderFactory(
                                                                                                           MimeTypeDetectors.class.getClassLoader());

    private final ComponentLibrary<MimeTypeDetector, MimeTypeDetectorConfig> library;
    private Logger logger;

    public MimeTypeDetectors() {
        library = new ComponentLibrary<MimeTypeDetector, MimeTypeDetectorConfig>(true);
        library.setClassLoaderFactory(DEFAULT_CLASSLOADER_FACTORY);
    }

    /**
     * Adds the configuration for a MIME-type detector <em>before</em> any previously added configurations, or updates any
     * existing one that represents the {@link MimeTypeDetectorConfig#equals(Object) same configuration}
     * 
     * @param config the new configuration; must not be <code>null</code>.
     * @return <code>true</code> if the detector was added, or <code>false</code> if there already was an existing detector
     *         configuration.
     * @see #removeDetector(MimeTypeDetectorConfig)
     */
    public boolean addDetector( MimeTypeDetectorConfig config ) {
        return library.add(config);
    }

    /**
     * Gets the class loader factory that should be used to load MIME-type detectors. By default, this service uses a factory that
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

    /**
     * Returns the first non-null result of iterating over the {@link #addDetector(MimeTypeDetectorConfig) registered} MIME-type
     * detectors in the reverse order in which they were registered to determine the MIME-type of a data source, using its
     * supplied content and/or its supplied name, depending upon the implementation. If the MIME-type cannot be determined by any
     * registered detector, "text/plain" or "application/octet-stream" will be returned, the former only if it is determined the
     * stream contains no nulls.
     * 
     * @param name The name of the data source; may be <code>null</code>.
     * @param content The content of the data source; may be <code>null</code>.
     * @return The MIME-type of the data source; never <code>null</code>.
     * @throws IOException If an error occurs reading the supplied content.
     */
    public String mimeTypeOf( String name,
                              InputStream content ) throws IOException {
        if (content != null && content.markSupported()) {
            content.mark(Integer.MAX_VALUE);
        }
        // Check if registered detectors can determine MIME-type
        for (MimeTypeDetector detector : library.getInstances()) {
            String mimeType = detector.mimeTypeOf(name, content);
            if (mimeType != null) return mimeType;
        }
        // If not, try to analyze stream to determine if it represents text or binary content
        if (content != null && content.markSupported()) {
            try {
                content.reset();
                for (int chr = content.read(); chr >= 0; chr = content.read()) {
                    if (chr == 0) return DEFAULT_MIME_TYPE;
                }
            } catch (IOException meansTooManyBytesRead) {
                return DEFAULT_MIME_TYPE;
            }
        }
        return "text/plain";
    }

    /**
     * Removes the configuration for a MIME-type detector.
     * 
     * @param config the configuration to be removed; must not be <code>null</code>.
     * @return <code>true</code> if the configuration was removed, or <code>false</code> if there was no existing configuration
     * @see #addDetector(MimeTypeDetectorConfig)
     */
    public boolean removeDetector( MimeTypeDetectorConfig config ) {
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
