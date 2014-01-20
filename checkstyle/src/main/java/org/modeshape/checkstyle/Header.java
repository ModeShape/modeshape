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

package org.modeshape.checkstyle;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * @author Randall Hauch (rhauch@redhat.com)
 */
public class Header extends com.puppycrawl.tools.checkstyle.checks.header.HeaderCheck {

    private Set<String> excludedFileSet;
    private String excludedFilesRegex;
    private Pattern excludedFilesPattern;
    private final String workingDirPath = new File(".").getAbsoluteFile().getParentFile().getAbsolutePath();
    private final int workingDirPathLength = workingDirPath.length();

    public Header() {
    }

    public void setExcludedFilesRegex( String excludedFilePattern ) {
        this.excludedFilesRegex = excludedFilePattern;
        this.excludedFilesPattern = Pattern.compile(this.excludedFilesRegex);
    }

    public void setExcludedClasses( String excludedClasses ) {
        this.excludedFileSet = new HashSet<>();
        if (excludedClasses != null) {
            for (String classname : excludedClasses.split(",")) {
                if (classname != null && classname.trim().length() != 0) {
                    String path = classname.trim().replace('.', '/') + ".java"; // change package names to filenames ...
                    this.excludedFileSet.add(path.trim());
                }
            }
        }
    }

    @Override
    public void setHeaderFile( String aFileName ) {
        // Load the file from the file ...
        InputStream stream = this.getClass().getClassLoader().getResourceAsStream("modeshape.header");
        if (stream == null) {
            throw new RuntimeException("unable to load header file (using classloader) " + aFileName);
        }
        // Load the contents and place into the lines ...
        try {
            final LineNumberReader lnr = new LineNumberReader(new InputStreamReader(stream));
            StringBuilder sb = new StringBuilder();
            while (true) {
                final String l = lnr.readLine();
                if (l == null) {
                    break;
                }
                sb.append(l).append("\\n");
            }
            super.setHeader(sb.toString());
        } catch (IOException e) {
            throw new RuntimeException("problem reading header file (using classloader) " + aFileName, e);
        }
    }

    protected boolean isExcluded( File file ) {
        // See whether this file is excluded ...
        String filename = file.getAbsolutePath().replace(File.separator, "/");
        if (filename.startsWith(workingDirPath)) filename = filename.substring(workingDirPathLength);
        filename = filename.replaceAll(".*/src/(main|test)/(java|resources)/", "");

        // First try one of the explicit class names ...
        for (String excludedFileName : excludedFileSet) {
            if (filename.endsWith(excludedFileName)) return true;
        }

        // Next try to evaluate the pattern ...
        if (excludedFilesPattern != null && excludedFilesPattern.matcher(filename).matches()) {
            return true;
        }
        return false;
    }

    @Override
    protected void processFiltered( File aFile,
                                    List<String> aLines ) {
        if (isExcluded(aFile)) return;
        super.processFiltered(aFile, aLines);
    }
}
