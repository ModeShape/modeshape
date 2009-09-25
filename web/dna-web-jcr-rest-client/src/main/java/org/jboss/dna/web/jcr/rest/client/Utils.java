/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * JBoss DNA is free software. Unless otherwise indicated, all code in JBoss DNA
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * JBoss DNA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.web.jcr.rest.client;

import java.io.File;
import java.io.InputStream;
import java.util.Map;
import org.jboss.dna.common.util.MimeTypeUtil;

/**
 * The <code>Utils</code> class contains common utilities used by this project.
 */
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
     */
    public static String getMimeType( File file ) {
        if (mimeTypeUtils == null) {
            // load custom extensions
            InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream("org/jboss/dna/web/jcr/rest/client/mime.types"); //$NON-NLS-1$
            Map<String, String> customMap = MimeTypeUtil.load(stream, null);

            // construct
            mimeTypeUtils = new MimeTypeUtil(customMap, true);
        }

        String mimeType = mimeTypeUtils.mimeTypeOf(file);
        return ((mimeType == null) ? "application/octet-stream" : mimeType); //$NON-NLS-1$
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
