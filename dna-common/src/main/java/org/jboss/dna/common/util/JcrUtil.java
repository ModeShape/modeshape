/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors. 
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.common.util;

/**
 * @author Serge Pagop
 *
 */
public class JcrUtil {
    
    /**
     * Create a path for the tree with index.
     * 
     * @param path the path.
     * @param index the index begin with 1.
     * @return the string
     * @throws IllegalArgumentException if the path is null, blank or empty, or if the index is not a positive value
     */
    public static String createPathWithIndex( String path,
                                        int index ) {
        ArgCheck.isNotEmpty(path, "path");
        ArgCheck.isPositive(index, "index");
        return path + "[" + index + "]";
    }

    /**
     * Create a path for the tree without index.
     * 
     * @param path - the path.
     * @return the string
     * @throws IllegalArgumentException if the path is null, blank or empty
     */
    public static String createPath( String path ) {
        ArgCheck.isNotEmpty(path, "path");
        return path;
    }
    
    private JcrUtil() {
        // prevent construction
    }

}
