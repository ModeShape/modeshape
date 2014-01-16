/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.modeshape.sequencer.ddl.standard;

import java.util.ArrayList;
import org.modeshape.common.text.ParsingException;
import org.modeshape.common.text.Position;
import org.modeshape.sequencer.ddl.Lexer;

/**
 *
 * @author kulikov
 */
public class Ddl extends Lexer {
    private StringBuilder text = new StringBuilder();
    private Position pos;
    
    private String command;
    private ArrayList<String> createOptions = new ArrayList();
    private String tableName;
    
    public Ddl() {
        super(Ddl.class.getResourceAsStream("/ddl-1.xml"));
    }

    public void reset(State state, String s, int i, int col, int row) {
        text.delete(0, text.length());
        pos = new Position(i, row, col);
    }
    
    public void resetAndRead(State state, String s, int i, int col, int row) {
        text.delete(0, text.length());
        text.append(s);
        pos = new Position(i, row, col);
    }

    public void read(State state, String s, int i, int col, int row) {
        text.append(s);
    }
    
    public void failure(State state, String s, int i, int col, int row) {
        throw new ParsingException(pos, "Syntax error between  " + text.toString() + " and " + s);
    }
    
    public void signal(State state, String s, int i, int col, int row) {
        signal(text.toString(), i, col, row);
    }

    public void storeCommand(State state, String s, int i, int col, int row) {
        command = s;
    }
    
    public void storeOption(State state, String s, int i, int col, int row) {
        createOptions.add(s);
    }

    public void storeTableName(State state, String s, int i, int col, int row) {
        tableName = s;
    }

    public void storeColumnName(State state, String s, int i, int col, int row) {
    }

    public void storeColumnTypeName(State state, String s, int i, int col, int row) {
    }

    public void recognizeDataType(State state, String s, int i, int col, int row) {
        signal(text.toString(), i, col, row);
        signal(s, i, col, row);
    }
    
}
