package org.modeshape.sequencer.ddl.dialect.sqlserver;

import org.modeshape.sequencer.ddl.DdlTokenStream;
import org.modeshape.sequencer.ddl.StandardDdlParser;

public class SqlServerDdlParser extends StandardDdlParser {

    private static final String[] COMMENT_ON = {"COMMENT", "ON"};
    private static final String TERMINATOR = "GO";

    public SqlServerDdlParser() {
        setTerminator(TERMINATOR);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.sequencer.ddl.StandardDdlParser#initializeTokenStream(org.modeshape.sequencer.ddl.DdlTokenStream)
     */
    @Override
    protected void initializeTokenStream( DdlTokenStream tokens ) {
        super.initializeTokenStream(tokens);
        tokens.registerStatementStartPhrase(COMMENT_ON);
    }
}
