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
package org.modeshape.common.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.modeshape.common.annotation.Immutable;

/**
 * A set of utilities for working with files and directories.
 */
@Immutable
public class FileUtil {

    private static FilenameFilter ACCEPT_ALL = new FilenameFilter() {

        @Override
        public boolean accept( File dir,
                               String name ) {
            return true;
        }
    };

    /**
     * Delete the file or directory at the supplied path. This method works on a directory that is not empty, unlike the
     * {@link File#delete()} method.
     * 
     * @param path the path to the file or directory that is to be deleted
     * @return true if the file or directory at the supplied path existed and was successfully deleted, or false otherwise
     */
    public static boolean delete( String path ) {
        if (path == null || path.trim().length() == 0) return false;
        return delete(new File(path));
    }

    /**
     * Delete the file or directory given by the supplied reference. This method works on a directory that is not empty, unlike
     * the {@link File#delete()} method.
     * 
     * @param fileOrDirectory the reference to the Java File object that is to be deleted
     * @return true if the supplied file or directory existed and was successfully deleted, or false otherwise
     */
    public static boolean delete( File fileOrDirectory ) {
        if (fileOrDirectory == null) return false;
        if (!fileOrDirectory.exists()) return false;

        // The file/directory exists, so if a directory delete all of the contents ...
        if (fileOrDirectory.isDirectory()) {
            File[] files = fileOrDirectory.listFiles();
            if (files != null) {
                for (File childFile : files) {
                    delete(childFile); // recursive call (good enough for now until we need something better)
                }
            }
            // Now an empty directory ...
        }
        // Whether this is a file or empty directory, just delete it ...
        return fileOrDirectory.delete();
    }

    /**
     * Copy the source file system structure into the supplied target location. If the source is a file, the destination will be
     * created as a file; if the source is a directory, the destination will be created as a directory.
     * 
     * @param sourceFileOrDirectory the file or directory whose contents are to be copied into the target location
     * @param destinationFileOrDirectory the location where the copy is to be placed; does not need to exist, but if it does its
     *        type must match that of <code>src</code>
     * @return the number of files (not directories) that were copied
     * @throws IllegalArgumentException if the <code>src</code> or <code>dest</code> references are null
     * @throws IOException
     */
    public static int copy( File sourceFileOrDirectory,
                            File destinationFileOrDirectory ) throws IOException {
        return copy(sourceFileOrDirectory, destinationFileOrDirectory, null);
    }

    /**
     * Copy the source file system structure into the supplied target location. If the source is a file, the destination will be
     * created as a file; if the source is a directory, the destination will be created as a directory.
     * 
     * @param sourceFileOrDirectory the file or directory whose contents are to be copied into the target location
     * @param destinationFileOrDirectory the location where the copy is to be placed; does not need to exist, but if it does its
     *        type must match that of <code>src</code>
     * @param exclusionFilter a filter that matches files or folders that should _not_ be copied; null indicates that all files
     *        and folders should be copied
     * @return the number of files (not directories) that were copied
     * @throws IllegalArgumentException if the <code>src</code> or <code>dest</code> references are null
     * @throws IOException
     */
    public static int copy( File sourceFileOrDirectory,
                            File destinationFileOrDirectory,
                            FilenameFilter exclusionFilter ) throws IOException {
        if (exclusionFilter == null) exclusionFilter = ACCEPT_ALL;
        int numberOfFilesCopied = 0;
        if (sourceFileOrDirectory.isDirectory()) {
            destinationFileOrDirectory.mkdirs();
            String list[] = sourceFileOrDirectory.list(exclusionFilter);

            for (int i = 0; i < list.length; i++) {
                String dest1 = destinationFileOrDirectory.getPath() + File.separator + list[i];
                String src1 = sourceFileOrDirectory.getPath() + File.separator + list[i];

                numberOfFilesCopied += copy(new File(src1), new File(dest1), exclusionFilter);
            }
        } else {
            try (FileInputStream fis = new FileInputStream(sourceFileOrDirectory);
                 BufferedInputStream bis = new BufferedInputStream(fis);
                 FileOutputStream fos = new FileOutputStream(destinationFileOrDirectory);
                 BufferedOutputStream bos = new BufferedOutputStream(fos)) {
                int c;
                while ((c = bis.read()) >= 0) {
                    bos.write(c);
                }
            }
            numberOfFilesCopied++;
        }
        return numberOfFilesCopied;
    }

    /**
     * Utility to convert {@link File} to {@link URL}.
     * 
     * @param filePath the path of the file
     * @return the {@link URL} representation of the file.
     * @throws MalformedURLException
     * @throws IllegalArgumentException if the file path is null, empty or blank
     */
    public static URL convertFileToURL( String filePath ) throws MalformedURLException {
        CheckArg.isNotEmpty(filePath, "filePath");
        File file = new File(filePath.trim());
        return file.toURI().toURL();
    }

    /**
     * Determines the size (in bytes) of the file or directory at the given path.
     *
     * @param filePath the path of the file; may not be {@code null}
     * @return the size in bytes of the file or the total computed size of the folder. If the given path is not a valid file or
     * folder, this will return 0.
     * @throws IOException if anything unexpected fails.
     */
    public static long size(String filePath) throws IOException {
        CheckArg.isNotEmpty(filePath, "filePath");
        File file = new File(filePath);
        if (!file.exists()) {
            return 0;
        }
        if (file.isFile()) {
            return file.length();
        }
        final AtomicLong size = new AtomicLong();
        Files.walkFileTree(Paths.get(filePath), new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile( Path file, BasicFileAttributes attrs ) {
                size.addAndGet(attrs.size());
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed( Path file, IOException exc ) {
                return FileVisitResult.CONTINUE;
            }
        });
        return size.get();
    }

    /**
     * Unzip archive to the specified destination.
     * 
     * @param zipFile zip archive
     * @param dest directory where archive will be uncompressed
     * @throws IOException 
     */
    public static void unzip(InputStream zipFile, String dest) throws IOException {
        byte[] buffer = new byte[1024];

        //create output directory is not exists
        File folder = new File(dest);

        if (folder.exists()) {
            FileUtil.delete(folder);
        }

        folder.mkdir();

        //get the zip file content
        try (ZipInputStream zis = new ZipInputStream(zipFile)) {
            //get the zipped file list entry
            ZipEntry ze = zis.getNextEntry();
            File parent = new File(dest);
            while (ze != null) {
                String fileName = ze.getName();
                if (ze.isDirectory()) {
                    File newFolder = new File(parent, fileName);
                    newFolder.mkdir();
                } else {
                    File newFile = new File(parent, fileName);
                    try (FileOutputStream fos = new FileOutputStream(newFile)) {
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }
                }
                ze = zis.getNextEntry();
            }

            zis.closeEntry();
        }
    }
    
    /**
     * Compresses directory into zip archive.
     * 
     * @param dirName the path to the directory
     * @param nameZipFile archive name.
     * @throws IOException 
     */
    public static void zipDir(String dirName, String nameZipFile) throws IOException {
        try (FileOutputStream fW = new FileOutputStream(nameZipFile);
             ZipOutputStream zip = new ZipOutputStream(fW)) {
            addFolderToZip("", dirName, zip);
        }
    }

    /**
     * Adds folder to the archive.
     * 
     * @param path path to the folder
     * @param srcFolder folder name
     * @param zip zip archive
     * @throws IOException 
     */
    public static void addFolderToZip(String path, String srcFolder, ZipOutputStream zip) throws IOException {
        File folder = new File(srcFolder);
        if (folder.list().length == 0) {
            addFileToZip(path, srcFolder, zip, true);
        } else {
            for (String fileName : folder.list()) {
                if (path.equals("")) {
                    addFileToZip(folder.getName(), srcFolder + "/" + fileName, zip, false);
                } else {
                    addFileToZip(path + "/" + folder.getName(), srcFolder + "/" + fileName, zip, false);
                }
            }
        }
    }

    /**
     * Appends file to the archive.
     * 
     * @param path path to the file
     * @param srcFile file name
     * @param zip archive
     * @param flag
     * @throws IOException 
     */
    public static void addFileToZip(String path, String srcFile, ZipOutputStream zip, boolean flag) throws IOException {
        File folder = new File(srcFile);
        if (flag) {
            zip.putNextEntry(new ZipEntry(path + "/" + folder.getName() + "/"));
        } else {
            if (folder.isDirectory()) {
                addFolderToZip(path, srcFile, zip);
            } else {
                byte[] buf = new byte[1024];
                int len;
                try (FileInputStream in = new FileInputStream(srcFile)) {
                    zip.putNextEntry(new ZipEntry(path + "/" + folder.getName()));
                    while ((len = in.read(buf)) > 0) {
                        zip.write(buf, 0, len);
                    }
                }
            }
        }
    }

    /**
     * Returns the extension of a file, including the dot.
     * 
     * @param filename the name of a file, may be not be null
     * @return the file extension or an empty string if the extension cannot be determined.
     */
    public static String getExtension(final String filename) {
        Objects.requireNonNull(filename, "filename cannot be null");
        int lastDotIdx = filename.lastIndexOf(".");
        return lastDotIdx >= 0 ? filename.substring(lastDotIdx) : "";
    }
    
    private FileUtil() {
    }

}
