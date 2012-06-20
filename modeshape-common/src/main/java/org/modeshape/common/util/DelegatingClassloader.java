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

package org.modeshape.common.util;

import org.modeshape.common.logging.Logger;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;

/**
 * Class loader which contains a list of classloaders to which it delegates each operation. If none of the delegates are able
 * to perform the operation, it delegates to the super-class( {@link URLClassLoader} ) which has most of the classloader methods
 * properly implemented.
 *
 * @author Horia Chiorean
 */
public final class DelegatingClassLoader extends URLClassLoader {

    private static final Logger LOGGER = Logger.getLogger(DelegatingClassLoader.class);

    private final List<? extends  ClassLoader> delegates;

    public DelegatingClassLoader( ClassLoader parent,
                                  List<? extends ClassLoader> delegates ) {
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
