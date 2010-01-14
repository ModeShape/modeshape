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
package org.modeshape.sequencer.text;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import net.jcip.annotations.ThreadSafe;
import org.modeshape.common.util.Logger;
import org.modeshape.graph.sequencer.SequencerOutput;
import org.modeshape.graph.sequencer.StreamSequencer;
import org.modeshape.graph.sequencer.StreamSequencerContext;

/**
 * The base class for the text sequencers. This class treats the text to be sequenced as a series of rows, with each row delimited
 * by a line terminator. Concrete subclasses provide their own mechanisms for splitting a row of data into a series of columns.
 * <p>
 * This class provides some fundamental capabilities, including the ability to set a {@link #setCommentMarker(String) comment
 * marker}, {@link #setMaximumLinesToRead(int) limit the number of lines} to be read from a file, and
 * {@link #setRowFactoryClassName(String) provide custom transformations} from the sets of columns to the graph structure.
 * </p>
 */
@ThreadSafe
public abstract class AbstractTextSequencer implements StreamSequencer {

    private final Logger logger = Logger.getLogger(getClass());
    private String rowFactoryClassName = null;
    private String commentMarker = null;
    private int maximumLinesToRead = -1;

    /**
     * {@inheritDoc}
     */
    public void sequence( InputStream stream,
                          SequencerOutput output,
                          StreamSequencerContext context ) {
        BufferedReader reader = null;
        String line = null;
        RowFactory rowFactory = null;
        String rowFactoryClassName = this.rowFactoryClassName;
        String commentMarker = this.commentMarker;
        int maxLinesToRead = this.maximumLinesToRead;

        int rowCount = 0;

        try {
            rowFactory = createRowFactory(rowFactoryClassName);
        } catch (Exception ex) {
            // This really shouldn't be able to happen, as we test-instantiate the class before it is set
            throw new IllegalStateException(TextSequencerI18n.couldNotInstantiateRowFactory.text(rowFactoryClassName));
        }

        try {
            reader = new BufferedReader(new InputStreamReader(stream));

            while ((line = reader.readLine()) != null) {
                if (commentMarker != null && line.startsWith(this.commentMarker)) continue;
                if ((maxLinesToRead > 0) && (++rowCount > maxLinesToRead)) return;
                String[] columns = parseLine(line);

                rowFactory.recordRow(context, output, columns);
            }

        } catch (IOException ioe) {
            logger.error(ioe, TextSequencerI18n.errorReadingLine, context.getInputPath());

        } finally {
            try {
                if (reader != null) reader.close();
            } catch (Exception ignore) {
            }
        }

    }

    /**
     * Sets the comment marker to use. Any line that begins with the comment marker will be ignored and will not be counted as a
     * read line for the purposes of the {@link #getMaximumLinesToRead() maximum line limitation}.
     * 
     * @param commentMarker the string that indicates that the line is a comment and should be ignored; null indicates that there
     *        is no comment marker
     */
    public void setCommentMarker( String commentMarker ) {
        this.commentMarker = commentMarker;
    }

    /**
     * @return the current comment marker; may be null
     */
    public String getCommentMarker() {
        return commentMarker;
    }

    /**
     * @return the maximum number of lines to read when sequencing; non-positive numbers indicate that all lines should be read
     *         and sequenced
     */
    public int getMaximumLinesToRead() {
        return maximumLinesToRead;
    }

    /**
     * Sets the maximum number of lines to read. When this number is reached during the sequencing of any particular stream, the
     * stream will be closed and remaining lines (if any) will be ignored. {@link #setCommentMarker(String) Comment lines} do not
     * count towards the number of lines read.
     * 
     * @param maximumLinesToRead the maximum number of lines to read; a non-positive number indicates that all lines should be
     *        read and sequenced.
     */
    public void setMaximumLinesToRead( int maximumLinesToRead ) {
        this.maximumLinesToRead = maximumLinesToRead;
    }

    /**
     * @return the current row factory class name; may not be null
     */
    public String getRowFactoryClassName() {
        return rowFactoryClassName;
    }

    /**
     * Sets the custom row factory class name. This method attempts to instantiate an instance of the custom {@link RowFactory}
     * class prior to modifying the row factory class name to ensure that the new value represents a valid implementation.
     * 
     * @param rowFactoryClassName the fully-qualified class name of the new custom row factory implementation; null indicates that
     *        {@link DefaultRowFactory the default row factory} should be used.
     * @throws ClassNotFoundException if the the named row factory class cannot be located
     * @throws IllegalAccessException if the row factory class or its nullary constructor is not accessible.
     * @throws InstantiationException if the row factory represents an abstract class, an interface, an array class, a primitive
     *         type, or void; or if the class has no nullary constructor; or if the instantiation fails for some other reason.
     * @throws ClassCastException if the instantiated row factory does not implement the {@link RowFactory} interface
     */
    public synchronized void setRowFactoryClassName( String rowFactoryClassName )
        throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        // Make sure that it's going to work
        createRowFactory(rowFactoryClassName);
        this.rowFactoryClassName = rowFactoryClassName;
    }

    /**
     * Parse the given row into its constituent columns.
     * 
     * @param row the row to be parsed
     * @return an array of columns; never null
     */
    protected abstract String[] parseLine( String row );

    /**
     * Creates an instance of the {@link #getRowFactoryClassName() row factory} configured for this sequencer.
     * 
     * @param className the name of the class to configure; null indicates that the {@link DefaultRowFactory default row factory}
     *        should be used.
     * @return an implementation of the named class; never null
     * @throws ClassNotFoundException if the the named row factory class cannot be located
     * @throws IllegalAccessException if the row factory class or its nullary constructor is not accessible.
     * @throws InstantiationException if the row factory represents an abstract class, an interface, an array class, a primitive
     *         type, or void; or if the class has no nullary constructor; or if the instantiation fails for some other reason.
     */
    protected synchronized RowFactory createRowFactory( String className )
        throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        if (this.rowFactoryClassName == null) {
            return new DefaultRowFactory();
        }

        Class<?> rowFactoryClass = Class.forName(this.rowFactoryClassName);
        RowFactory rowFactory = (RowFactory)rowFactoryClass.newInstance();

        return rowFactory;
    }
}
