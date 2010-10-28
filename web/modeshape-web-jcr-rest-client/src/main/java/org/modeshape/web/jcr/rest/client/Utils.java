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
package org.modeshape.web.jcr.rest.client;

import java.io.File;
import java.io.InputStream;
import java.util.Map;
import org.modeshape.common.util.MimeTypeUtil;

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
        	ClassLoader cl = Thread.currentThread().getContextClassLoader();
        	try {	        		        	
	        	Thread.currentThread().setContextClassLoader(Utils.class.getClassLoader());
	        	
	            InputStream stream =Thread.currentThread().getContextClassLoader().getResourceAsStream("org/modeshape/web/jcr/rest/client/mime.types");
	            Map<String, String> customMap = MimeTypeUtil.load(stream, null);
	
	            // construct
	            mimeTypeUtils = new MimeTypeUtil(customMap, true);
        	} finally {
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
