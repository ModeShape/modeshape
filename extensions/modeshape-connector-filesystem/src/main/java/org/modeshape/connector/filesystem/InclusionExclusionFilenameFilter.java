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
package org.modeshape.connector.filesystem;

import java.io.File;
import java.util.regex.Pattern;

/**
 * Provides basic filename filter implementation for simple inclusion/exclusion pattern.
 *
 * @author johnament
 */
public class InclusionExclusionFilenameFilter implements java.io.FilenameFilter {
    private String inclusionPattern = null;
    private String exclusionPattern = null;
    private Pattern inclusion;
    private Pattern exclusion;

    public void setExclusionPattern(String exclusionPattern) {
        this.exclusionPattern = exclusionPattern;
        if(exclusionPattern == null) {
            this.exclusion = null;
        } else {
            this.exclusion = Pattern.compile(exclusionPattern);
        }
    }

    public void setInclusionPattern(String inclusionPattern) {
        this.inclusionPattern = inclusionPattern;
        if(inclusionPattern == null) {
            this.inclusion = null;
        } else {
            this.inclusion = Pattern.compile(inclusionPattern);
        }
    }

    public String getExclusionPattern() {
        return exclusionPattern;
    }

    public String getInclusionPattern() {
        return inclusionPattern;
    }

    @Override
    public boolean accept(File file, String string) {
        if(inclusionPattern == null && exclusionPattern == null) {
            return true;
        } else if (inclusionPattern == null && exclusionPattern != null) {
            return !exclusion.matcher(string).matches();
            //return !string.matches(exclusionPattern);
        } else {
            if(exclusionPattern == null) {
                return inclusion.matcher(string).matches();
//               return string.matches(inclusionPattern);
            }
            return inclusion.matcher(string).matches() &&
                    !exclusion.matcher(string).matches();
            //return string.matches(inclusionPattern) && !string.matches(exclusionPattern);
        }
    }

}
