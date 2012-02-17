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
package org.modeshape.sequencer.ddl.datatype;

import org.modeshape.sequencer.ddl.DdlConstants;

/**
 * A representation of SQL data types.
 */
public class DataType {
    private String name;
    private long length = -1;
    private int precision = -1;
    private int scale = -1;

    /**
     * The statement source.
     */
    private String source = "";

    public DataType() {
        super();
    }

    public DataType( String theName ) {
        super();
        this.name = theName;
    }

    public DataType( String name,
                     int length ) {
        super();
        this.name = name;
        this.length = length;
    }

    public DataType( String name,
                     int precision,
                     int scale ) {
        super();
        this.name = name;
        this.precision = precision;
        this.scale = scale;
    }

    public String getName() {
        return this.name;
    }

    public void setName( String value ) {
        this.name = value;
    }

    public void setLength( long value ) {
        this.length = value;
    }

    public long getLength() {
        return this.length;
    }

    public void setPrecision( int value ) {
        this.precision = value;
    }

    public int getPrecision() {
        return this.precision;
    }

    public int getScale() {
        return this.scale;
    }

    public void setScale( int value ) {
        this.scale = value;
    }

    @Override
    public String toString() {
        StringBuffer result = new StringBuffer(100);
        result.append("DataType()").append(" ").append(name);

        return result.toString();
    }

    /**
     * @param source
     */
    public void setSource( String source ) {
        if (source == null) {
            source = "";
        }
        this.source = source;
    }

    /**
     * @return source string
     */
    public String getSource() {
        return source;
    }

    /**
     * @param addSpaceBefore
     * @param value
     */
    public void appendSource( boolean addSpaceBefore,
                              String value ) {
        if (addSpaceBefore) {
            this.source = this.source + DdlConstants.SPACE;
        }
        this.source = this.source + value;
    }

    /**
     * @param addSpaceBefore
     * @param value
     * @param additionalStrs
     */
    public void appendSource( boolean addSpaceBefore,
                              String value,
                              String... additionalStrs ) {
        if (addSpaceBefore) {
            this.source = this.source + DdlConstants.SPACE;
        }
        this.source = this.source + value;
    }

}
