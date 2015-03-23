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
package org.modeshape.common.util;

import java.io.InputStream;

/**
 * Utility to search and load resources from the CP.
 *
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public final class ResourceLookup {

    private ResourceLookup() {
    }

    /**
     * Returns the stream of a resource at a given path, using some optional class loaders.
     *
     * @param path the path to search
     * @param classLoader a {@link java.lang.ClassLoader} instance to use when searching; may be null.
     * @param useTLCL {@code true} if the thread local class loader should be used as well, in addition to {@code classLoader}
     * @return a {@link java.io.InputStream} if the resource was found, or {@code null} otherwise
     */
    public static InputStream read( String path, ClassLoader classLoader, boolean useTLCL ) {
        if (useTLCL) {
            InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
            if (stream != null) {
                return stream;
            }
        }
        return classLoader != null ? classLoader.getResourceAsStream(path) : ResourceLookup.class.getResourceAsStream(path);
    }

    /**
     * Returns the stream of a resource at a given path, using the CL of a class.
     *
     * @param path the path to search
     * @param clazz a {@link java.lang.Class} instance which class loader should be used when doing the lookup.
     * @param useTLCL {@code true} if the thread local class loader should be used as well, in addition to {@code classLoader}
     * @return a {@link java.io.InputStream} if the resource was found, or {@code null} otherwise
     * @see org.modeshape.common.util.ResourceLookup#read(String, ClassLoader, boolean)
     */
    public static InputStream read( String path, Class<?> clazz, boolean useTLCL ) {
        return read(path, clazz.getClassLoader(), useTLCL);
    }
}
