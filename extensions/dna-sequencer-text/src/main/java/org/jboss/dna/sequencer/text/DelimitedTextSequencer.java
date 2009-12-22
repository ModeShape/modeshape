package org.jboss.dna.sequencer.text;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.jboss.dna.common.util.CheckArg;

/**
 * An text sequencer implementation that uses a {@link #setSplitPattern(String) regular-expression pattern} to split incoming rows
 * into columns.  By default, this class uses the pattern {@code ","} to parse files on commas.
 * 
 * @see AbstractTextSequencer
 * @see Pattern
 */
public class DelimitedTextSequencer extends AbstractTextSequencer {

    private Pattern splitPattern = Pattern.compile(",");

    /**
     * Sets the regular expression to use to split incoming rows.
     * 
     * @param regularExpression the regular expression to use to split incoming rows; may not be null.
     * @throws PatternSyntaxException if {@code regularExpression} does not represent a valid regular expression that can be
     *         {@link Pattern#compile(String) compiled into a pattern}.
     */
    public void setSplitPattern( String regularExpression ) throws PatternSyntaxException {
        CheckArg.isNotNull(regularExpression, "regularExpression");

        splitPattern = Pattern.compile(regularExpression);
    }

    @Override
    protected String[] parseLine( String line ) {
        return splitPattern.split(line);
    }

}
