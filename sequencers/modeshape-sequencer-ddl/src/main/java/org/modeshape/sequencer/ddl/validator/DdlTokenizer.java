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
package org.modeshape.sequencer.ddl.validator;

import java.util.ArrayList;

/**
 *
 * @author kulikov
 */
public class DdlTokenizer {
    private final static int TOKEN = 1;
    private final static int WHITESPACE = 3;
    
    private int state;
    
    public String[] stream(String text) {
        this.state = WHITESPACE;
        
        ArrayList<String> stream = new ArrayList();
        StringBuilder token = new StringBuilder();
        
        for (int i = 0; i < text.length(); i++) {
            switch (state) {
                case TOKEN :
                    switch(text.charAt(i)) {
                        case ' ' :
                            add(stream, token);
                            state = WHITESPACE;
                            break;
                        case ')' :
                        case '(' :
                        case ',' :
                            add(stream, token);
                            stream.add(text.substring(i, i + 1));
                            state = WHITESPACE;
                            break;
                        default :
                            token.append(text.charAt(i));
                            state = TOKEN;
                            break;
                    }
                    break;
                case WHITESPACE :
                    switch(text.charAt(i)) {
                        case ' ' :
                            break;
                        case ')' :
                        case '(' :
                        case ',' :
                            add(stream, token);
                            stream.add(text.substring(i, i + 1));
                            state = TOKEN;
                            break;
                        default :
                            token.append(text.charAt(i));
                            state = TOKEN;
                            break;
                    }
                    break;
            }
        }
        
        String[] res = new String[stream.size()];
        stream.toArray(res);
        
        return res;
    }
    
    private void add(ArrayList<String> tokens, StringBuilder token) {
        if (token.length() > 0) {
            tokens.add(token.toString().trim());
        }
        token.delete(0, token.length());
    }
}
