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
package org.modeshape.jcr;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.modeshape.common.collection.UnmodifiableProperties;

/**
 * Information about ModeShape.
 */
public final class ModeShape {

    private static final Properties bundleProperties = loadBundleProperties();

    private static Properties loadBundleProperties() {
        // This is idempotent, so we don't need to lock ...
        InputStream stream = null;
        try {
            stream = JcrRepository.class.getClassLoader().getResourceAsStream("org/modeshape/jcr/repository.properties");
            assert stream != null;
            Properties props = new Properties();
            props.load(stream);
            return new UnmodifiableProperties(props);
        } catch (IOException e) {
            throw new IllegalStateException(JcrI18n.failedToReadPropertiesFromManifest.text(e.getLocalizedMessage()), e);
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                } finally {
                    stream = null;
                }
            }
        }
    }

    /**
     * Get the name suitable for public display.
     * 
     * @return the name; never null
     */
    public static final String getName() {
        return bundleProperties.getProperty("name");
    }

    /**
     * Get the vendor name suitable for public display.
     * 
     * @return the name; never null
     */
    public static final String getVendor() {
        return bundleProperties.getProperty("vendor");
    }

    /**
     * Get the project URL suitable for public display.
     * 
     * @return the name; never null
     */
    public static final String getUrl() {
        return bundleProperties.getProperty("url");
    }

    /**
     * Get the version suitable for public display.
     * 
     * @return the name; never null
     */
    public static final String getVersion() {
        return bundleProperties.getProperty("version");
    }

}
