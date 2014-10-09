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

import java.net.URL;
import java.net.URLClassLoader;
import org.modeshape.common.logging.Logger;

/**
 * Class loader which contains a list of classloaders to which it delegates each operation. If none of the delegates are able
 * to perform the operation, it delegates to the super-class( {@link URLClassLoader} ) which has most of the classloader methods
 * properly implemented.
 *
 * @author Horia Chiorean
 */
public final class DelegatingClassLoader extends URLClassLoader {

    private static final Logger LOGGER = Logger.getLogger(DelegatingClassLoader.class);

    private final Iterable<? extends  ClassLoader> delegates;

    public DelegatingClassLoader( ClassLoader parent,
                                  Iterable<? extends  ClassLoader> delegates ) {
        super(new URL[0], parent);
        CheckArg.isNotNull(delegates, "delegates");
        this.delegates = delegates;
    }

    @Override
    protected Class<?> findClass( String name ) throws ClassNotFoundException {
        for (ClassLoader delegate : delegates) {
            try {
                return delegate.loadClass(name);
            } catch (ClassNotFoundException e) {
                LOGGER.debug(e, "Cannot load class using delegate: " + delegate.getClass().toString());
            }
        }
        return super.findClass(name);
    }


    @Override
    public URL findResource( String name ) {
        for (ClassLoader delegate : delegates) {
            try {
                return delegate.getResource(name);
            } catch (Exception e) {
                LOGGER.debug(e, "Cannot load resource using delegate: " + delegate.getClass().toString());
            }
        }
        return super.findResource(name);
    }
}
