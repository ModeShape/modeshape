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
package org.modeshape.jdbc.util;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.modeshape.jdbc.JdbcI18n;

/**
 * Utilities for string processing and manipulation.
 */
public class StringUtil {

    public static final String[] EMPTY_STRING_ARRAY = new String[0];
    private static final Pattern PARAMETER_COUNT_PATTERN = Pattern.compile("\\{(\\d+)\\}");



    /**
     * Create a string by substituting the parameters into all key occurrences in the supplied format. The pattern consists of
     * zero or more keys of the form <code>{n}</code>, where <code>n</code> is an integer starting at 1. Therefore, the first
     * parameter replaces all occurrences of "{1}", the second parameter replaces all occurrences of "{2}", etc.
     * <p>
     * If any parameter is null, the corresponding key is replaced with the string "null". Therefore, consider using an empty
     * string when keys are to be removed altogether.
     * </p>
     * <p>
     * If there are no parameters, this method does nothing and returns the supplied pattern as is.
     * </p>
     * 
     * @param pattern the pattern
     * @param parameters the parameters used to replace keys
     * @return the string with all keys replaced (or removed)
     */
    public static String createString( String pattern,
                                       Object... parameters ) {
        CheckArg.isNotNull(pattern, "pattern");
        if (parameters == null) parameters = EMPTY_STRING_ARRAY;
        Matcher matcher = PARAMETER_COUNT_PATTERN.matcher(pattern);
        StringBuffer text = new StringBuffer();
        int requiredParameterCount = 0;
        boolean err = false;
        while (matcher.find()) {
            int ndx = Integer.valueOf(matcher.group(1));
            if (requiredParameterCount <= ndx) {
                requiredParameterCount = ndx + 1;
            }
            if (ndx >= parameters.length) {
                err = true;
                matcher.appendReplacement(text, matcher.group());
            } else {
                Object parameter = parameters[ndx];

                // Automatically pretty-print arrays
                if (parameter != null && parameter.getClass().isArray()) {
                    parameter = Arrays.asList((Object[])parameter);
                }

                matcher.appendReplacement(text, Matcher.quoteReplacement(parameter == null ? "null" : parameter.toString()));
            }
        }
        if (err || requiredParameterCount < parameters.length) {
            throw new IllegalArgumentException(
                                               JdbcI18n.i18nRequiredToSuppliedParameterMismatch.text(parameters.length,
                                                                                                   parameters.length == 1 ? "" : "s",
                                                                                                   requiredParameterCount,
                                                                                                   requiredParameterCount == 1 ? "" : "s",
                                                                                                   pattern,
                                                                                                   text.toString()));
        }
        matcher.appendTail(text);

        return text.toString();
    }

    /**
     * Create a new string containing the specified character repeated a specific number of times.
     * 
     * @param charToRepeat the character to repeat
     * @param numberOfRepeats the number of times the character is to repeat in the result; must be greater than 0
     * @return the resulting string
     */
    public static String createString( final char charToRepeat,
                                       int numberOfRepeats ) {
        assert numberOfRepeats >= 0;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < numberOfRepeats; ++i) {
            sb.append(charToRepeat);
        }
        return sb.toString();
    }

    private StringUtil() {
        // Prevent construction
    }
}
