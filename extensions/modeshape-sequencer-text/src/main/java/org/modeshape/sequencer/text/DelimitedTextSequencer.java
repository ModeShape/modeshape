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
package org.modeshape.sequencer.text;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.modeshape.common.util.CheckArg;

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
