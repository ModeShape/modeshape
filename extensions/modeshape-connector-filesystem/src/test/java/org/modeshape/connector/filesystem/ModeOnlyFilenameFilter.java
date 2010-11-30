package org.modeshape.connector.filesystem;

import java.io.File;
import java.io.FilenameFilter;

/**
 *
 * @author johnament
 */
public class ModeOnlyFilenameFilter implements FilenameFilter {

    @Override
    public boolean accept(File file, String string) {
        return string.endsWith(".mode");
    }

}
