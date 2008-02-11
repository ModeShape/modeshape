package org.jboss.dna.common.util;

import java.io.File;

public class FileUtil {

	/**
	 * Delete the file or directory at the supplied path.
	 * This method works on a directory that is not empty, unlike the {@link File#delete()} method.
	 * @param path the path to the file or directory that is to be deleted
	 * @return true if the file or directory at the supplied path existed and was successfully
	 * deleted, or false otherwise
	 */
	public static boolean delete( String path ) {
		if ( path == null || path.trim().length() == 0 ) return false;
		return delete(new File(path));
	}
	
	/**
	 * Delete the file or directory given by the supplied reference.
	 * This method works on a directory that is not empty, unlike the {@link File#delete()} method.
	 * @param fileOrDirectory the reference to the Java File object that is to be deleted
	 * @return true if the supplied file or directory existed and was successfully deleted,
	 * or false otherwise
	 */
	public static boolean delete( File fileOrDirectory ) {
		if ( fileOrDirectory == null ) return false;
		if ( !fileOrDirectory.exists() ) return false;
		
		// The file/directory exists, so if a directory delete all of the contents ...
		if ( fileOrDirectory.isDirectory() ) {
			for( File childFile : fileOrDirectory.listFiles() ) {
				delete(childFile);		// recursive call (good enough for now until we need something better)
			}
			// Now an empty directory ...
		}
		// Whether this is a file or empty directory, just delete it ...
		return fileOrDirectory.delete();
	}
	
}
