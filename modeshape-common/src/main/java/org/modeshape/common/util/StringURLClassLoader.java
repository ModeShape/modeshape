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
