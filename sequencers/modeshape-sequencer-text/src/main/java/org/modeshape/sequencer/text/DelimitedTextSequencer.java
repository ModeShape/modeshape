/*
 * ModeShape (http://www.modeshape.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.modeshape.sequencer.text;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.modeshape.common.util.CheckArg;

/**
 * An text sequencer implementation that uses a {@link #setSplitPattern(String) regular-expression pattern} to split incoming rows
 * into columns.  By default, this class uses the pattern {@code ","} to parse files on commas.
 * 
 * @see AbstractTextSequencer
 * @see java.util.regex.Pattern
 */
public class DelimitedTextSequencer extends AbstractTextSequencer {

    private String splitPattern = ",";

    /**
     * Sets the regular expression to use to split incoming rows.
     * 
     * @param regularExpression the regular expression to use to split incoming rows; may not be null.
     * @throws java.util.regex.PatternSyntaxException if {@code regularExpression} does not represent a valid regular expression that can be
     *         {@link java.util.regex.Pattern#compile(String) compiled into a pattern}.
     */
    public void setSplitPattern( String regularExpression ) throws PatternSyntaxException {
        CheckArg.isNotNull(regularExpression, "regularExpression");
        Pattern.compile(splitPattern);
        splitPattern = regularExpression;
    }

    @Override
    protected String[] parseLine( String line ) {
        return line.split(splitPattern);
    }

}
