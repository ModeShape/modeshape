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
package org.modeshape.sequencer.ddl.datatype;

import org.modeshape.sequencer.ddl.DdlConstants;

/**
 * A representation of SQL data types.
 */
public class DataType {

    public static final long DEFAULT_LENGTH = -1;
    public static final int DEFAULT_PRECISION = -1;
    public static final int DEFAULT_SCALE = -1;
    public static final int DEFAULT_ARRAY_DIMENSIONS = 0;

    private String name;
    private long length = DEFAULT_LENGTH;
    private int precision = DEFAULT_PRECISION;
    private int scale = DEFAULT_SCALE;
    private int arrayDimensions = DEFAULT_ARRAY_DIMENSIONS;
    private boolean autoIncrement = false;
    private boolean notNull = false;

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

    /**
     * @return array dimensions
     */
    public int getArrayDimensions() {
        return arrayDimensions;
    }

    /**
     * @param arrayDimensions
     */
    public void setArrayDimensions(int arrayDimensions) {
        this.arrayDimensions = arrayDimensions;
    }
    /**
     * @return auto increment
     */
    public boolean isAutoIncrement() {
        return this.autoIncrement;
    }

    /**
     * @param autoIncrement the autoIncrement flag to set
     */
    public void setAutoIncrement(boolean autoIncrement) {
        this.autoIncrement = autoIncrement;
    }

    /**
     * @return not null flag
     */
    public boolean isNotNull() {
        return this.notNull;
    }

    /**
     * @param notNull the notNull flag to set
     */
    public void setNotNull(boolean notNull) {
        this.notNull = notNull;
    }
    @Override
    public String toString() {
        StringBuilder result = new StringBuilder(100);
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
