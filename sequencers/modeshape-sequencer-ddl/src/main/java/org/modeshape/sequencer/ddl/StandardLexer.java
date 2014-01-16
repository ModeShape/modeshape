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
package org.modeshape.sequencer.ddl;

import org.modeshape.common.text.ParsingException;
import org.modeshape.common.text.Position;
import org.modeshape.sequencer.ddl.standard.CreateStatement;

/**
 * Standard DDL syntaxt lexer.
 * 
 * @author kulikov
 */
public class StandardLexer extends Lexer {
    private Lexer createStatement;
    /**
     * Creates new instance.
     */
    public StandardLexer() {
        super(StandardLexer.class.getResourceAsStream("/ddl.xml"));
        createStatement = new CreateStatement();
    }
    
    @Override
    public void setListener(ErrorListener listener) {
        super.setListener(listener);
        createStatement.setListener(listener);
    }

    public void triggerForCommand(State state, String c, int i, int col, int row) {
        text.delete(0, text.length());
        text.append(c);
        setPosition(i, col, row);
    }
    
    public void readCommand(State state, String c, int i, int col, int row) {
        text.append(c);
    }
    
    public void testCommand(State state, String c, int i, int col, int row) {
        signal(text.toString(), i, col, row);
    }

    public void failOnCommand(State state, String c, int i, int col, int row) {
        throw new ParsingException(position, "Wrong command name: " + text);
    }

    public void failOnDigit(State state, String c, int i, int col, int row) {
        throw new ParsingException(new Position(i, row, col), "Digits are not allowed here: " + c);
    }
    
    /**
     * Delegates characters to the create statement lexer.
     * 
     * @param state
     * @param s 
     */
    public void createStatement(State state, String s, int i, int col, int row) {
        createStatement.signal(s, i, col, row);
    }
    
}
