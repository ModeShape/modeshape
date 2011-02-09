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
package org.modeshape.jcr;

import java.io.File;
import java.io.PrintStream;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.jcr.PropertyType;
import javax.jcr.Value;
import org.modeshape.common.util.StringUtil;
import org.modeshape.graph.property.Path;

/**
 * A profiling utility for recording various activities.
 */
public class Profiler {

    protected static final char SPACE = ' ';
    protected static final char QUOTE = '\'';
    protected static final char DOUBLE_QUOTE = '\'';
    public static final String PROFILE_FILENAME_PROPERTY_NAME = "org.modeshape.jcr.profile.filename";
    public static final String DEFAULT_FILENAME = "modeshape-profile.log";
    private static PrintStream output;
    private static final Lock lock = new ReentrantLock();
    private static int counter = 0;

    public static void activated() {
        String outputFilename = System.getProperty(PROFILE_FILENAME_PROPERTY_NAME);
        if (outputFilename != null) outputFilename = outputFilename.trim();
        if (outputFilename == null || outputFilename.length() == 0) {
            outputFilename = DEFAULT_FILENAME;
        }
        if (outputFilename.endsWith("/")) {
            outputFilename = outputFilename + DEFAULT_FILENAME;
        }
        try {
            lock.lock();
            File outputFile = null;
            do {
                outputFile = new File(outputFilename + '.' + StringUtil.justifyRight(Integer.toString(++counter), 3, '0'));
            } while (outputFile.exists());
            assert outputFile != null;
            assert !outputFile.exists();
            System.out.println("Writing profile output to " + outputFile.getAbsolutePath());
            File parent = outputFile.getParentFile();
            if (parent != null) {
                parent.mkdirs();
                assert parent.canWrite();
            }
            output = new PrintStream(outputFile);
        } catch (Throwable e) {
            e.printStackTrace();
            output = System.out;
        } finally {
            lock.unlock();
        }
    }

    public static void deactivated() {
        output.flush();
        if (output != System.out) {
            try {
                lock.lock();
                output.close();
            } finally {
                output = null;
                lock.unlock();
            }
        }
    }

    public void enterMethod( String methodName,
                             Object[] targetAndParams ) {
        writeMethodOnLine("ENTER", null, methodName, targetAndParams);
    }

    public void exitMethod( String methodName,
                            Object[] targetAndParams ) {
        writeMethodOnLine("EXIT", null, methodName, targetAndParams);
    }

    public void traceMethod( String methodName,
                             Object[] targetAndParams ) {
        writeMethodOnLine(null, null, methodName, targetAndParams);
    }

    public void enterMethod( Object target,
                             String methodName,
                             Object... params ) {
        writeMethodOnLine("ENTER", target, methodName, params);
    }

    public void exitMethod( Object target,
                            String methodName,
                            Object... params ) {
        writeMethodOnLine("EXIT", target, methodName, params);
    }

    public void traceMethod( Object target,
                             String methodName,
                             Object... params ) {
        writeMethodOnLine(null, target, methodName, params);
    }

    public void trace( String action,
                       Object... params ) {
        writeMethodOnLine(action, null, null, params);
    }

    public void trace( String action ) {
        writeMethodOnLine(action, null, null);
    }

    public void traceMessage( String message ) {
        writeOnLine(message);
    }

    protected final void writeOnLine( String message ) {
        try {
            lock.lock();
            output.print(message);
            output.println();
        } catch (Throwable t) {
            t.printStackTrace(output);
        } finally {
            lock.unlock();
        }
    }

    protected final void writeMethodOnLine( String action,
                                            Object target,
                                            String methodName,
                                            Object... params ) {
        writeMethod(true, action, target, methodName, params);

    }

    protected final void writeMethod( boolean appendNewline,
                                      String action,
                                      Object target,
                                      String methodName,
                                      Object... params ) {
        try {
            lock.lock();
            output.print(now());
            output.append(SPACE);
            output.print(Thread.currentThread().getId());
            output.append(SPACE);
            if (action != null) {
                output.print(action);
                output.append(SPACE);
            }
            int i = 0;
            if (target == null && params.length > 0) {
                target = params[0];
                i = 1;
            }
            if (target != null) {
                output.print(target.getClass().getName());
                output.append('.');
            }
            if (methodName != null) {
                output.print(methodName);
                output.append('(');
            }
            boolean first = true;
            int maxIndex = params.length;
            for (; i != maxIndex; ++i) {
                Object value = params[i];
                if (first) first = false;
                else output.print(',');
                write(value);
            }
            if (methodName != null) {
                output.append(')');
            }
            if (target != null) {
                output.append(" on ");
                write(target);
            }
            output.println();
        } catch (Throwable t) {
            t.printStackTrace(output);
        } finally {
            lock.unlock();
        }
    }

    protected final void write( Object value ) throws Exception {
        if (value instanceof String) {
            output.print(DOUBLE_QUOTE);
            output.append(value.toString());
            output.print(DOUBLE_QUOTE);
        } else if (value instanceof Path) {
            output.print(QUOTE);
            output.append(value.toString());
            output.print(QUOTE);
        } else if (value instanceof Value) {
            Value v = (Value)value;
            switch (v.getType()) {
                case PropertyType.STRING:
                    output.print(DOUBLE_QUOTE);
                    output.append(v.toString());
                    output.print(DOUBLE_QUOTE);
                    break;
                case PropertyType.PATH:
                    output.print(QUOTE);
                    output.append(v.toString());
                    output.print(QUOTE);
                    break;
                case PropertyType.BINARY:
                    output.append(v.getBinary().toString());
                    break;
                default:
                    output.append(v.toString());
                    break;
            }
        } else if (value instanceof JcrSession) {
            output.print("Session ");
            output.append(((JcrSession)value).sessionId());
        } else if (value instanceof JcrWorkspace) {
            output.print("Workspace ");
            output.append(((JcrWorkspace)value).getName());
        } else {
            output.print(value);
        }

    }

    protected static final long now() {
        return System.nanoTime();
    }
}
