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
package org.modeshape.web.jcr.rest.client;

import java.io.File;
import java.io.InputStream;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Map;
import org.modeshape.common.util.MimeTypeUtil;

/**
 * The <code>Utils</code> class contains common utilities used by this project.
 */
@SuppressWarnings( "deprecation" )
public final class Utils {

    // ===========================================================================================================================
    // Class Fields
    // ===========================================================================================================================

    // utility to detect file mime type by using file extension
    private static MimeTypeUtil mimeTypeUtils;

    // ===========================================================================================================================
    // Class Methods
    // ===========================================================================================================================

    /**
     * @param thisObj an object being compared (may be <code>null</code>)
     * @param thatObj the other object being compared (may be <code>null</code>)
     * @return <code>true</code> if both objects are <code>null</code> or both are not <code>null</code> and equal
     */
    public static boolean equivalent( Object thisObj,
                                      Object thatObj ) {
        // true if both objects are null
        if (thisObj == null) {
            return (thatObj == null);
        }

        if (thatObj == null) return false;
        return thisObj.equals(thatObj);
    }

    /**
     * @param file the file whose mime type is being requested
     * @return the mime type or the default mime type (<code>"application/octet-stream"</code>) if one can't be determined from
     *         the file extension (never <code>null</code>)
     * @deprecated Use another MIME type detection framework, such as Tika
     */
    @Deprecated
    public static String getMimeType( File file ) {
        if (mimeTypeUtils == null) {
            // load custom extensions
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(Utils.class.getClassLoader());

                final ClassLoader classLoader = AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
                    @Override
                    public ClassLoader run() {
                        return Thread.currentThread().getContextClassLoader();
                    }
                });

                InputStream stream = classLoader.getResourceAsStream("org/modeshape/web/jcr/rest/client/mime.types");
                Map<String, String> customMap = MimeTypeUtil.load(stream, null);

                // construct
                mimeTypeUtils = new MimeTypeUtil(customMap, true);
            } finally {
                // restore original classloader
                Thread.currentThread().setContextClassLoader(cl);
            }
        }

        String mimeType = mimeTypeUtils.mimeTypeOf(file);
        return ((mimeType == null) ? "application/octet-stream" : mimeType);
    }

    // ===========================================================================================================================
    // Constructors
    // ===========================================================================================================================

    /**
     * Don't allow construction.
     */
    public Utils() {
        // nothing to do
    }

}
