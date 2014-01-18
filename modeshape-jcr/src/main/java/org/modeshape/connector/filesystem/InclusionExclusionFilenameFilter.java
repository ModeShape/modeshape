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
package org.modeshape.connector.filesystem;

import java.io.File;
import java.io.FilenameFilter;
import java.util.regex.Pattern;

/**
 * A {@link FilenameFilter} implementation that supports an inclusion and exclusion pattern.
 */
class InclusionExclusionFilenameFilter implements java.io.FilenameFilter {
    private String inclusionPattern = null;
    private String exclusionPattern = null;
    private Pattern inclusion;
    private Pattern exclusion;
    private Pattern extraPropertiesExclusion;

    public void setExclusionPattern( String exclusionPattern ) {
        this.exclusionPattern = exclusionPattern;
        if (exclusionPattern == null) {
            this.exclusion = null;
        } else {
            this.exclusion = Pattern.compile(exclusionPattern);
        }
    }

    public void setExtraPropertiesExclusionPattern( String exclusionPattern ) {
        if (exclusionPattern == null) {
            this.extraPropertiesExclusion = null;
        } else {
            this.extraPropertiesExclusion = Pattern.compile(exclusionPattern);
        }
    }

    public void setInclusionPattern( String inclusionPattern ) {
        this.inclusionPattern = inclusionPattern;
        if (inclusionPattern == null) {
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
    public boolean accept( File file,
                           String name ) {
        if (inclusionPattern == null) {
            // Include unless it matches an exclusion ...
            if (exclusionPattern != null && exclusion.matcher(name).matches()) return false;
            if (extraPropertiesExclusion != null && extraPropertiesExclusion.matcher(name).matches()) return false;
            return true;
        }
        // Include ONLY if it matches the inclusion AND not matched by the exclusions ...
        if (!inclusion.matcher(name).matches()) return false;
        if (exclusionPattern != null && exclusion.matcher(name).matches()) return false;
        if (extraPropertiesExclusion != null && extraPropertiesExclusion.matcher(name).matches()) return false;
        return true;
    }
}
