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

/**
 * Validator for DDL syntax.
 * 
 * @author kulikov
 */
public class DdlValidator implements Lexer.ErrorListener {
    private Lexer lexer;
    private int row, col;
    
    private String message;
    
    /**
     * Creates validator with default lexer.
     */
    public DdlValidator() {
        lexer = new StandardLexer(this);
    }
    
    /**
     * Creates validator with a specific lexer.
     * 
     * @param lexer the lexer instance.
     */
    public DdlValidator(Lexer lexer) {
        this.lexer = lexer;
        this.lexer.setListener(this);
    }
    
    /**
     * Runs validation process.
     * 
     * @param ddl ddl string.
     */
    public void validate(String ddl) throws ParsingException {
        //reset row and column indexes;
        row = 0; col = 0;
        message = null;
        
        //return lexer to initial state
        lexer.reset();
        
        //process ddl stream character by character
        for (int i = 0; i < ddl.length(); i++) {
            //gettting next character
            String ch = ddl.substring(i, i+1);
            
            //line separator?
            if (ch.equals("\n")) {
                //increment row index and reset column index
                row++;
                col = 0;
                
                //signal white spaces
                lexer.signal(" ");
            } else {
                //send character                
                lexer.signal(ch);
            }
            
            if (message != null) {
                throw new ParsingException(new Position(i, row, col), message);
            }
            //increment column index
            col++;
        }
        
        //we are done with stream so send the 
        //special termination signal
        
        lexer.signal("eos");        
        
    }

    @Override
    public void onError(String message) {
        this.message = message;
    }
}
