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
package org.modeshape.common.util;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import org.modeshape.common.annotation.Immutable;

/**
 * A set of utilities for more easily performing I/O.
 */
@Immutable
public class IoUtil {

    /**
     * Read and return the entire contents of the supplied {@link InputStream stream}. This method always closes the stream when
     * finished reading.
     * 
     * @param stream the stream to the contents; may be null
     * @return the contents, or an empty byte array if the supplied reader is null
     * @throws IOException if there is an error reading the content
     */
    public static byte[] readBytes( InputStream stream ) throws IOException {
        if (stream == null) return new byte[] {};
        byte[] buffer = new byte[1024];
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        boolean error = false;
        try {
            int numRead = 0;
            while ((numRead = stream.read(buffer)) > -1) {
                output.write(buffer, 0, numRead);
            }
        } catch (IOException e) {
            error = true; // this error should be thrown, even if there is an error closing stream
            throw e;
        } catch (RuntimeException e) {
            error = true; // this error should be thrown, even if there is an error closing stream
            throw e;
        } finally {
            try {
                stream.close();
            } catch (IOException e) {
                if (!error) throw e;
            }
        }
        output.flush();
        return output.toByteArray();
    }

    /**
     * Read and return the entire contents of the supplied {@link File file}.
     * 
     * @param file the file containing the contents; may be null
     * @return the contents, or an empty byte array if the supplied file is null
     * @throws IOException if there is an error reading the content
     */
    public static byte[] readBytes( File file ) throws IOException {
        if (file == null) return new byte[] {};
        InputStream stream = new BufferedInputStream(new FileInputStream(file));
        boolean error = false;
        try {
            return readBytes(stream);
        } catch (IOException e) {
            error = true; // this error should be thrown, even if there is an error closing stream
            throw e;
        } catch (RuntimeException e) {
            error = true; // this error should be thrown, even if there is an error closing stream
            throw e;
        } finally {
            try {
                stream.close();
            } catch (IOException e) {
                if (!error) throw e;
            }
        }
    }

    /**
     * Read and return the entire contents of the supplied {@link Reader}. This method always closes the reader when finished
     * reading.
     * 
     * @param reader the reader of the contents; may be null
     * @return the contents, or an empty string if the supplied reader is null
     * @throws IOException if there is an error reading the content
     */
    public static String read( Reader reader ) throws IOException {
        if (reader == null) return "";
        StringBuilder sb = new StringBuilder();
        boolean error = false;
        try {
            int numRead = 0;
            char[] buffer = new char[1024];
            while ((numRead = reader.read(buffer)) > -1) {
                sb.append(buffer, 0, numRead);
            }
        } catch (IOException e) {
            error = true; // this error should be thrown, even if there is an error closing reader
            throw e;
        } catch (RuntimeException e) {
            error = true; // this error should be thrown, even if there is an error closing reader
            throw e;
        } finally {
            try {
                reader.close();
            } catch (IOException e) {
                if (!error) throw e;
            }
        }
        return sb.toString();
    }

    /**
     * Read and return the entire contents of the supplied {@link InputStream}. This method always closes the stream when finished
     * reading.
     * 
     * @param stream the streamed contents; may be null
     * @return the contents, or an empty string if the supplied stream is null
     * @throws IOException if there is an error reading the content
     */
    public static String read( InputStream stream ) throws IOException {
        return stream == null ? "" : read(new InputStreamReader(stream));
    }

    /**
     * Read and return the entire contents of the supplied {@link File}.
     * 
     * @param file the file containing the information to be read; may be null
     * @return the contents, or an empty string if the supplied reader is null
     * @throws IOException if there is an error reading the content
     */
    public static String read( File file ) throws IOException {
        if (file == null) return "";
        StringBuilder sb = new StringBuilder();
        boolean error = false;
        Reader reader = new FileReader(file);
        try {
            int numRead = 0;
            char[] buffer = new char[1024];
            while ((numRead = reader.read(buffer)) > -1) {
                sb.append(buffer, 0, numRead);
            }
        } catch (IOException e) {
            error = true; // this error should be thrown, even if there is an error closing reader
            throw e;
        } catch (RuntimeException e) {
            error = true; // this error should be thrown, even if there is an error closing reader
            throw e;
        } finally {
            try {
                reader.close();
            } catch (IOException e) {
                if (!error) throw e;
            }
        }
        return sb.toString();
    }

    /**
     * Write the entire contents of the supplied string to the given file.
     * 
     * @param content the content to write to the stream; may be null
     * @param file the file to which the content is to be written
     * @throws IOException
     * @throws IllegalArgumentException if the stream is null
     */
    public static void write( String content,
                              File file ) throws IOException {
        CheckArg.isNotNull(file, "destination file");
        if (content != null) {
            write(content, new FileOutputStream(file));
        }
    }

    /**
     * Write the entire contents of the supplied string to the given stream. This method always flushes and closes the stream when
     * finished.
     * 
     * @param content the content to write to the stream; may be null
     * @param stream the stream to which the content is to be written
     * @throws IOException
     * @throws IllegalArgumentException if the stream is null
     */
    public static void write( String content,
                              OutputStream stream ) throws IOException {
        CheckArg.isNotNull(stream, "destination stream");
        boolean error = false;
        try {
            if (content != null) {
                byte[] bytes = content.getBytes();
                stream.write(bytes, 0, bytes.length);
            }
        } catch (IOException e) {
            error = true; // this error should be thrown, even if there is an error flushing/closing stream
            throw e;
        } catch (RuntimeException e) {
            error = true; // this error should be thrown, even if there is an error flushing/closing stream
            throw e;
        } finally {
            try {
                stream.flush();
            } catch (IOException e) {
                if (!error) throw e;
            } finally {
                try {
                    stream.close();
                } catch (IOException e) {
                    if (!error) throw e;
                }
            }
        }
    }

    /**
     * Write the entire contents of the supplied string to the given writer. This method always flushes and closes the writer when
     * finished.
     * 
     * @param content the content to write to the writer; may be null
     * @param writer the writer to which the content is to be written
     * @throws IOException
     * @throws IllegalArgumentException if the writer is null
     */
    public static void write( String content,
                              Writer writer ) throws IOException {
        CheckArg.isNotNull(writer, "destination writer");
        boolean error = false;
        try {
            if (content != null) {
                writer.write(content);
            }
        } catch (IOException e) {
            error = true; // this error should be thrown, even if there is an error flushing/closing writer
            throw e;
        } catch (RuntimeException e) {
            error = true; // this error should be thrown, even if there is an error flushing/closing writer
            throw e;
        } finally {
            try {
                writer.flush();
            } catch (IOException e) {
                if (!error) throw e;
            } finally {
                try {
                    writer.close();
                } catch (IOException e) {
                    if (!error) throw e;
                }
            }
        }
    }

    /**
     * Write the entire contents of the supplied string to the given stream. This method always flushes and closes the stream when
     * finished.
     * 
     * @param input the content to write to the stream; may be null
     * @param stream the stream to which the content is to be written
     * @throws IOException
     * @throws IllegalArgumentException if the stream is null
     */
    public static void write( InputStream input,
                              OutputStream stream ) throws IOException {
        write(input, stream, 1024);
    }

    /**
     * Write the entire contents of the supplied string to the given stream. This method always flushes and closes the stream when
     * finished.
     * 
     * @param input the content to write to the stream; may be null
     * @param stream the stream to which the content is to be written
     * @param bufferSize the size of the buffer; must be positive
     * @throws IOException
     * @throws IllegalArgumentException if the stream is null
     */
    public static void write( InputStream input,
                              OutputStream stream,
                              int bufferSize ) throws IOException {
        CheckArg.isNotNull(stream, "destination stream");
        CheckArg.isPositive(bufferSize, "bufferSize");
        boolean error = false;
        try {
            if (input != null) {
                byte[] buffer = new byte[bufferSize];
                try {
                    int numRead = 0;
                    while ((numRead = input.read(buffer)) > -1) {
                        stream.write(buffer, 0, numRead);
                    }
                } finally {
                    input.close();
                }
            }
        } catch (IOException e) {
            error = true; // this error should be thrown, even if there is an error flushing/closing stream
            throw e;
        } catch (RuntimeException e) {
            error = true; // this error should be thrown, even if there is an error flushing/closing stream
            throw e;
        } finally {
            try {
                stream.flush();
            } catch (IOException e) {
                if (!error) throw e;
            } finally {
                try {
                    stream.close();
                } catch (IOException e) {
                    if (!error) throw e;
                }
            }
        }
    }

    /**
     * Write the entire contents of the supplied string to the given writer. This method always flushes and closes the writer when
     * finished.
     * 
     * @param input the content to write to the writer; may be null
     * @param writer the writer to which the content is to be written
     * @throws IOException
     * @throws IllegalArgumentException if the writer is null
     */
    public static void write( Reader input,
                              Writer writer ) throws IOException {
        CheckArg.isNotNull(writer, "destination writer");
        boolean error = false;
        try {
            if (input != null) {
                char[] buffer = new char[1024];
                try {
                    int numRead = 0;
                    while ((numRead = input.read(buffer)) > -1) {
                        writer.write(buffer, 0, numRead);
                    }
                } finally {
                    input.close();
                }
            }
        } catch (IOException e) {
            error = true; // this error should be thrown, even if there is an error flushing/closing writer
            throw e;
        } catch (RuntimeException e) {
            error = true; // this error should be thrown, even if there is an error flushing/closing writer
            throw e;
        } finally {
            try {
                writer.flush();
            } catch (IOException e) {
                if (!error) throw e;
            } finally {
                try {
                    writer.close();
                } catch (IOException e) {
                    if (!error) throw e;
                }
            }
        }
    }

    /**
     * Write the entire contents of the supplied string to the given stream. This method always flushes and closes the stream when
     * finished.
     * 
     * @param input1 the first stream
     * @param input2 the second stream
     * @return true if the streams contain the same content, or false otherwise
     * @throws IOException
     * @throws IllegalArgumentException if the stream is null
     */
    public static boolean isSame( InputStream input1,
                                  InputStream input2 ) throws IOException {
        CheckArg.isNotNull(input1, "input1");
        CheckArg.isNotNull(input2, "input2");
        boolean error = false;
        try {
            byte[] buffer1 = new byte[1024];
            byte[] buffer2 = new byte[1024];
            try {
                int numRead1 = 0;
                int numRead2 = 0;
                while (true) {
                    numRead1 = input1.read(buffer1);
                    numRead2 = input2.read(buffer2);
                    if (numRead1 > -1) {
                        if (numRead2 != numRead1) return false;
                        // Otherwise same number of bytes read
                        if (!Arrays.equals(buffer1, buffer2)) return false;
                        // Otherwise same bytes read, so continue ...
                    } else {
                        // Nothing more in stream 1 ...
                        return numRead2 < 0;
                    }
                }
            } finally {
                input1.close();
            }
        } catch (IOException e) {
            error = true; // this error should be thrown, even if there is an error closing stream 2
            throw e;
        } catch (RuntimeException e) {
            error = true; // this error should be thrown, even if there is an error closing stream 2
            throw e;
        } finally {
            try {
                input2.close();
            } catch (IOException e) {
                if (!error) throw e;
            }
        }
    }

    /**
     * Get the {@link InputStream input stream} to the resource given by the supplied path. If a class loader is supplied, the
     * method attempts to resolve the resource using the {@link ClassLoader#getResourceAsStream(String)} method; if the result is
     * non-null, it is returned. Otherwise, if a class is supplied, this method attempts to resolve the resource using the
     * {@link Class#getResourceAsStream(String)} method; if the result is non-null, it is returned. Otherwise, this method then
     * uses the Class' ClassLoader to load the resource; if non-null, it is returned . Otherwise, this method looks for an
     * existing and readable {@link File file} at the path; if found, a buffered stream to that file is returned. Otherwise, this
     * method attempts to parse the resource path into a valid {@link URL}; if this succeeds, the method attempts to open a stream
     * to that URL. If all of these fail, this method returns null.
     * 
     * @param resourcePath the logical path to the classpath, file, or URL resource
     * @param clazz the class that should be used to load the resource as a stream; may be null
     * @param classLoader the classloader that should be used to load the resource as a stream; may be null
     * @return an input stream to the resource; or null if the resource could not be found
     * @throws IllegalArgumentException if the resource path is null or empty
     */
    public static InputStream getResourceAsStream( String resourcePath,
                                                   ClassLoader classLoader,
                                                   Class<?> clazz ) {
        CheckArg.isNotEmpty(resourcePath, "resourcePath");
        InputStream result = null;
        if (classLoader != null) {
            // Try using the class loader first ...
            result = classLoader.getResourceAsStream(resourcePath);
        }
        if (result == null && clazz != null) {
            // Not yet found, so try the class ...
            result = clazz.getResourceAsStream(resourcePath);
            if (result == null) {
                // Not yet found, so try the class's class loader ...
                result = clazz.getClassLoader().getResourceAsStream(resourcePath);
            }
        }
        if (result == null) {
            // Still not found, so see if this is an existing File ...
            try {
                File file = new File(resourcePath);
                if (file.exists() && file.canRead()) {
                    return new BufferedInputStream(new FileInputStream(file));
                }
            } catch (FileNotFoundException e) {
                // just continue ...
            }
        }
        if (result == null) {
            // Still not found, so try to construct a URL out of it ...
            try {
                URL url = new URL(resourcePath);
                return url.openStream();
            } catch (MalformedURLException e) {
                // just continue ...
            } catch (IOException err) {
                // just continue ...
            }
        }
        // Couldn't find it anywhere ...
        return result;
    }

    private IoUtil() {
        // Prevent construction
    }
}
