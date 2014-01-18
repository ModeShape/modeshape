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
package org.modeshape.jcr;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.modeshape.common.collection.UnmodifiableProperties;
import org.modeshape.common.logging.Logger;

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

    static {
        Logger.getLogger(JcrRepository.class).info(JcrI18n.initializing, getName(), getVersion());
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
