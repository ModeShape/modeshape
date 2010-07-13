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
package org.modeshape.jdbc.util;

import org.modeshape.jdbc.JdbcI18n;

/**
 * Utility class that checks arguments to methods. This class is to be used only in API methods, where failure to supply correct
 * arguments should result in a useful error message. In all cases, use the <code>assert</code> statement.
 */
public final class CheckArg {

    // ########################## int METHODS ###################################

    /**
     * Check that the specified argument is non-null
     * 
     * @param argument The argument
     * @param name The name of the argument
     * @throws IllegalArgumentException If argument is null
     */
    public static void isNotNull( Object argument,
                                  String name ) {
        if (argument == null) {
            throw new IllegalArgumentException(JdbcI18n.argumentMayNotBeNull.text(name));
        }
    }

    private CheckArg() {
        // prevent construction
    }
}
