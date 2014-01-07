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
package org.modeshape.sequencer.ddl.standard;

import org.modeshape.common.text.ParsingException;
import org.modeshape.common.text.Position;
import org.modeshape.sequencer.ddl.Lexer;

/**
 *
 * @author kulikov
 */
public class ColumnType extends Lexer {
   
    private Lexer parent;
    
    private StringBuilder type = new StringBuilder();
    private Position pos;
    
    private static final String TYPES = "(CHAR\\s*\\(\\s*\\d+\\s*\\))|" + 
        "(CHARACTER\\s*\\(\\s*\\d+\\s*\\))|" +
        "(VARCHAR\\s*\\(\\s*\\d+\\s*\\))|" +
        "(CHARACTER\\s+VARYING\\s*\\(\\s*\\d+\\s*\\))|" +
        "(VARCHAR2\\s*\\(\\s*\\d+\\s*\\))|" +
        "(INTEGER)|(INT)|(SMALLINT)|" +
        "(DECIMAL\\s*\\(\\s*\\d+\\s*,\\s*\\d+\\s*\\))|"+
        "(DEC\\s*\\(\\s*\\d+\\s*,\\s*\\d+\\s*\\))|" +
        "(NUMERIC\\s*\\(\\s*\\d+\\s*,\\s*\\d+\\s*\\))|" +
        "(FLOAT\\s*\\(\\s*\\d+\\s*\\))|" +
        "(REAL)|(DOUBLE PRECISION)|" +
        "(DATE)|(TIME)]" +
        "(INTERVAL \\d{4}[-]\\d{2})|(INTERVAL \\d\\d)|" +
        "(BOOLEAN)|(BLOB)";
    
    /**
     * 
     * @param parent 
     */
    public ColumnType(Lexer parent) {
        super(ColumnType.class.getResourceAsStream("/col_type.xml"));
        this.parent = parent;
    }
    
    /**
     * The name read by this lexer.
     * 
     * @return 
     */
    public String type() {
        return type.toString();
    }
    
    /**
     * First character has been detected.
     * 
     * @param state
     * @param s
     * @param i
     * @param col
     * @param row 
     */
    public void start(State state, String s, int i, int col, int row) {
        type.delete(0, type.length());
        pos = new Position(i, row, col);
        type.append(s);
    }
    
    /**
     * Reads until white space.
     * 
     * @param state
     * @param s
     * @param i
     * @param col
     * @param row 
     */
    public void readType(State state, String s, int i, int col, int row) {
        type.append(s);
    }

    /**
     * 
     * @param state
     * @param s
     * @param i
     * @param col
     * @param row 
     */
    public void testType(State state, String s, int i, int col, int row) {
        signal(type.toString(), i, col, row);
        signal(s, i, col, row);
    }
    
    /**
     * Sends error signal.
     * 
     * @param state
     * @param s
     * @param i
     * @param col
     * @param row 
     */
    public void fail(State state, String s, int i, int col, int row) {
        throw new ParsingException(pos, "Illegal character: " + s);
    }
    
    /**
     * Sends notification to the upper layer.
     * 
     * @param state
     * @param s
     * @param i
     * @param col
     * @param row 
     */
    public void notify(State state, String s, int i, int col, int row) {
//        if (type.toString().toLowerCase().matches(TYPES.toLowerCase())) {
            parent.signal("success", i, col, row);
//        } else {
//            throw new ParsingException(pos, "Unknown type definition: " + type);
//        }
    }
}
