package org.modeshape.sequencer.ddl;


import org.modeshape.sequencer.ddl.Lexer;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author kulikov
 */
public class Tester extends Lexer {
    private Lexer lexer;
    private boolean success = false;
    
    public Tester() {
        super(Tester.class.getResourceAsStream("/tester.xml"));
    }
    
    @Override
    public void reset() {
        success = false;
        if (lexer != null) {
            lexer.reset();
        }
        super.reset();
    }
    
    public void subordinate(Lexer lexer) {
        this.lexer = lexer;
    }
    
    public void delegate(State state, String s, int i, int col, int row) {
        lexer.signal(s, i, col, row);
    }

    public void success(State state, String s, int i, int col, int row) {
        success = true;
    }
    
    public boolean isSuccess() {
        return success;
    }
}
