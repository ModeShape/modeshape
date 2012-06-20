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
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;

/**
 * Extension of a {@link URLClassLoader} which accepts a list of strings instead of urls, trying to convert each string to an
 * url. If a string cannot be converted to a URL, it is discarded.
 *
 * @author Horia Chiorean
 */
public final class StringURLClassLoader extends URLClassLoader {

    private static final Logger LOGGER = Logger.getLogger(StringURLClassLoader.class);

    public StringURLClassLoader( List<String> urls ) {
        super(new URL[0], null);
        CheckArg.isNotNull(urls, "urls");
        for (String url : urls) {
            try {
                super.addURL(new URL(url));
            } catch (MalformedURLException e) {
                LOGGER.debug("{0} is not a valid url ", url);
            }
        }
    }
}
