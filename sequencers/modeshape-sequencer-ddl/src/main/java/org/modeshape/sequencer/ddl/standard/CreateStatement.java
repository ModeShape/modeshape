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

import org.modeshape.sequencer.ddl.Lexer;

/**
 *
 * @author kulikov
 */
public class CreateStatement extends Lexer {
    
    private CreateTableStatement createTableStatement;
    
    public CreateStatement(ErrorListener listener) {
        super(listener, CreateStatement.class.getResourceAsStream("/create_statement.xml"));
        createTableStatement = new CreateTableStatement(listener);
    }
    
    public void subjectAnalysis(State state, String s) {
        String token = token();
        resetReader();
        System.out.println(token);
        signal(token);
    }
    
    public void createTableStatement(State state, String s) {
        createTableStatement.signal(s);
    }
    
}
