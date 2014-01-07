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

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.modeshape.common.text.ParsingException;
import org.modeshape.common.text.Position;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Generic finite automata lexer.
 * 
 * @author kulikov
 */
public class Lexer {

    public interface ErrorListener {
        public void onError(String message);
    }
    
    private final ArrayList<State> states = new ArrayList();
    private State state;
    
    private StringBuilder tokenReader = new StringBuilder();
    protected final StringBuilder text = new StringBuilder();
    protected Position position;
    
    private ErrorListener listener;
    
    /**
     * Creates new lexer with given automata descriptor.
     * 
     * @param def automata xml descriptor as input stream.
     */
    public Lexer(InputStream def) {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        State is = null;
        try {

            //Using factory get an instance of document builder
            DocumentBuilder db = dbf.newDocumentBuilder();

            //parse using builder to get DOM representation of the XML file
            Document dom = db.parse(def);
            
            //select state nodes
            NodeList list = dom.getDocumentElement().getElementsByTagName("state");
            
            //load states
            loadStates(list);
            
            //transitions
            for (int i = 0; i < list.getLength(); i++) {
                NodeList children = list.item(i).getChildNodes();
                loadTransitions(list.item(i).getAttributes().getNamedItem("name").getNodeValue(), children);
            }
        } catch (ParserConfigurationException pce) {
            pce.printStackTrace();
        } catch (SAXException se) {
            se.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        
        this.reset();
    }
    
    /**
     * Loads states of the automata.
     * 
     * @param nodes states descriptors.
     */
    private void loadStates(NodeList nodes) {
        for (int i = 0; i < nodes.getLength(); i++) {
            NamedNodeMap attributes = nodes.item(i).getAttributes();
            String stateName = attributes.getNamedItem("name").getNodeValue();
            states.add(new State(stateName));
        }
    }
    
    /**
     * Instantiates transitions.
     * 
     * @param nodes 
     */
    private void loadTransitions(String stateName, NodeList nodes) {
        for (int j = 0; j < nodes.getLength(); j++) {
            if (nodes.item(j).getNodeName().equals("char")) {
                NamedNodeMap attributes = nodes.item(j).getAttributes();
                
                String expression = attributes.getNamedItem("expression").getNodeValue();
                String action = attributes.getNamedItem("action").getNodeValue();
                String dest = attributes.getNamedItem("destination").getNodeValue();
                Transition t = new Transition(this, expression, dest, action);
                findState(stateName).add(t);
            }
        }
    }
    
    /**
     * Resets this lexer to the initial state.
     */
    public void reset() {
        this.state = states.get(0);
        this.resetReader();
    }
    
    /**
     * Tests result of the syntax check procedure.
     * 
     * @return true if end states was reached and false otherwise.
     */
    public final boolean isCompleted() {
        return this.state == states.get(states.size() - 1);
    }
    
    
    /**
     * Assigns listener.
     * 
     * @param listener listener instance
     */
    public void setListener(ErrorListener listener) {
        this.listener = listener;
    }
    
    /**
     * Listener accessor.
     * 
     * @return 
     */
    protected ErrorListener listener() {
        return listener;
    }
    
    /**
     * Searches state with given name.
     * 
     * @param name
     * @return 
     */
    private State findState(String name) {
        for (State state : states) {
            if (state.name.equals(name)) {
                return state;
            }
        }
        return null;
    }
    
    
    /**
     * Processes character signal.
     * 
     * @param c 
     */
    public void signal(String c, int i, int col, int row) {
        if (state != null) {
            state.signal(c, i, col, row);
        }
    }
    
    /**
     * Parses given statement.
     * 
     * @param statement to parse.
     */
    public void parse(String statement) {
        reset();
        int row = 1; int col = 0;
        for (int i = 0; i < statement.length(); i++) {
            String ch = statement.substring(i, i+1);
            if (ch.equals("\n")) {
                ch = " ";
                row++; col = 0;
            }
            signal(ch, i, col, row);
        }
        signal("eos", 0, col, row);
    }
    
    public final void setPosition(int i, int col, int row) {
        this.position = new Position(i, row, col);
    }
    
    public void resetReader() {
        tokenReader.delete(0, tokenReader.length());
    }
    
    public void collect(State state, String s, int col, int row) {
        tokenReader.append(s);
    }

    public void fail(State state, String s) {
        listener().onError("Unpected token: " + s);
    }
    
    public String token() {
        return tokenReader.toString();
    }
    
    /**
     * Represents lexer's state.
     */
    public class State {
        private String name;
        private ArrayList<Transition> transitions = new ArrayList();
        
        public State(String name) {
            this.name = name;
        }
        
        public void add(Transition t) {
            transitions.add(t);
        }
        
        protected void signal(String c, int i, int col, int row) throws ParsingException {
//            System.out.println("State=" + state.name + ", signal=" + c);
            for (Transition t : transitions) {
                if (c.toLowerCase().matches(t.name)) {
                    System.out.println("State=" + state.name +",signal=" + c +  ", transition=" + t.name);
                    state = t.destination;
                    t.process(this, c, i, col, row);
                    
                    return;
                }
            }
            
            throw new IllegalStateException("Unexpected token: " + c);
        }
        
    }
    
    
    private class Transition {
        private String name;
        private State destination;
        private String action;
        private Lexer lexer;
        
        public Transition(Lexer lexer, String name, String dest, String action) {
            this.lexer = lexer;
            this.name = name;
            destination = findState(dest);
            this.action = action;
        }
        
        public void process(State state, String s, int i, int col, int row) throws ParsingException {
            if (action == null || action.length() == 0) {
                return;
            }
            try {
                Method m = lexer.getClass().getMethod(action, State.class, String.class, int.class, int.class, int.class);
                m.invoke(lexer, state, s, i, col, row);
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (IllegalAccessException ex) {                
                ex.printStackTrace();
            } catch (InvocationTargetException e) {
                if (e.getCause() instanceof ParsingException) {
                    throw (ParsingException)e.getCause();
                }
                e.printStackTrace();
            }
        }
    }
}
