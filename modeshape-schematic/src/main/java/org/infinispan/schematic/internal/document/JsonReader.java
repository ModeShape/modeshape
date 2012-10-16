/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.infinispan.schematic.internal.document;

import static org.infinispan.schematic.document.Json.ReservedField.BASE_64;
import static org.infinispan.schematic.document.Json.ReservedField.BINARY_TYPE;
import static org.infinispan.schematic.document.Json.ReservedField.CODE;
import static org.infinispan.schematic.document.Json.ReservedField.DATE;
import static org.infinispan.schematic.document.Json.ReservedField.INCREMENT;
import static org.infinispan.schematic.document.Json.ReservedField.OBJECT_ID;
import static org.infinispan.schematic.document.Json.ReservedField.REGEX_OPTIONS;
import static org.infinispan.schematic.document.Json.ReservedField.REGEX_PATTERN;
import static org.infinispan.schematic.document.Json.ReservedField.SCOPE;
import static org.infinispan.schematic.document.Json.ReservedField.TIMESTAMP;
import static org.infinispan.schematic.document.Json.ReservedField.UUID;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.infinispan.schematic.document.Bson.BinaryType;
import org.infinispan.schematic.document.Document;
import org.infinispan.schematic.document.DocumentSequence;
import org.infinispan.schematic.document.Immutable;
import org.infinispan.schematic.document.Json;
import org.infinispan.schematic.document.NotThreadSafe;
import org.infinispan.schematic.document.Null;
import org.infinispan.schematic.document.ParsingException;
import org.infinispan.schematic.document.ThreadSafe;
import org.infinispan.util.Base64;

/**
 * A class that reads the <a href="http://www.json.org/">JSON</a> data format and constructs an in-memory <a
 * href="http://bsonspec.org/">BSON</a> representation.
 * <p>
 * This reader is capable of optionally introspecting string values to look for certain string patterns that are commonly used to
 * represent dates. In introspection is not done by default, but when it is used it looks for the following patterns:
 * <ul>
 * <li>a string literal date of the form <code>"<i>yyyy</i>-<i>MM</i>-<i>dd</i>T<i>HH</i>:<i>mm</i>:<i>ss</i>"</code> where
 * <code>T</code> is a literal character</li>
 * <li>a string literal date of the form <code>"<i>yyyy</i>-<i>MM</i>-<i>dd</i>T<i>HH</i>:<i>mm</i>:<i>ss</i>Z"</code> where
 * <code>T</code> and <code>Z</code> are literal characters</li>
 * <li>a string literal date of the form
 * <code>"<i>yyyy</i>-<i>MM</i>-<i>dd</i>T<i>HH</i>:<i>mm</i>:<i>ss</i>GMT+<i>00</i>:<i>00</i>"</code> where <code>T</code>, and
 * <code>GMT</code> are literal characters</li>
 * <li>a string literal date of the form <code>"/Date(<i>millisOrIso</i>)/"</code></li>
 * <li>a string literal date of the form <code>"\/Date(<i>millisOrIso</i>)\/"</code></li>
 * </ul>
 * Note that in the date forms listed above, <code><i>millisOrIso</i></code> is either a long value representing the number of
 * milliseconds since epoch or a string literal in ISO-8601 format representing a date and time.
 * </p>
 * <p>
 * This reader also accepts non-string values that are function calls of the form
 * 
 * <pre>
 *     new <i>functionName</i>(<i>parameters</i>)
 * </pre>
 * 
 * where <code><i>parameters</i></code> consists of one or more JSON values (including nested functions). If the function call
 * cannot be parsed and executed, the string literal form of the function call is kept.
 * </p>
 * 
 * @author Randall Hauch <rhauch@redhat.com> (C) 2011 Red Hat Inc.
 * @since 5.1
 */
@ThreadSafe
@Immutable
public class JsonReader {

    protected static final DocumentValueFactory VALUE_FACTORY = new DefaultDocumentValueFactory();
    protected static final ValueMatcher SIMPLE_VALUE_MATCHER = new SimpleValueMatcher(VALUE_FACTORY);
    protected static final ValueMatcher DATE_VALUE_MATCHER = new DateValueMatcher(VALUE_FACTORY);

    public static final boolean DEFAULT_INTROSPECT = true;

    /**
     * Read the JSON representation from supplied URL and construct the {@link Document} representation, using the
     * {@link Charset#defaultCharset() default character set}.
     * 
     * @param url the URL to the JSON document; may not be null and must be resolvable
     * @return the in-memory {@link Document} representation
     * @throws ParsingException if there was a problem reading from the URL
     */
    public Document read( URL url ) throws ParsingException {
        try {
            return read(url.openStream(), DEFAULT_INTROSPECT);
        } catch (IOException e) {
            throw new ParsingException(e.getMessage(), e, 0, 0);
        }
    }

    /**
     * Read the JSON representation from supplied input stream and construct the {@link Document} representation, using the
     * {@link Charset#defaultCharset() default character set}.
     * 
     * @param stream the input stream; may not be null
     * @return the in-memory {@link Document} representation
     * @throws ParsingException if there was a problem reading from the stream
     */
    public Document read( InputStream stream ) throws ParsingException {
        return read(stream, DEFAULT_INTROSPECT);
    }

    /**
     * Read the JSON representation from supplied input stream and construct the {@link Document} representation, using the
     * supplied {@link Charset character set}.
     * 
     * @param stream the input stream; may not be null
     * @param charset the character set that should be used; may not be null
     * @return the in-memory {@link Document} representation
     * @throws ParsingException if there was a problem reading from the stream
     */
    public Document read( InputStream stream,
                          Charset charset ) throws ParsingException {
        return read(stream, charset, DEFAULT_INTROSPECT);
    }

    /**
     * Read the JSON representation from supplied input stream and construct the {@link Document} representation.
     * 
     * @param reader the IO reader; may not be null
     * @return the in-memory {@link Document} representation
     * @throws ParsingException if there was a problem reading from the stream
     */
    public Document read( Reader reader ) throws ParsingException {
        return read(reader, DEFAULT_INTROSPECT);
    }

    /**
     * Read the JSON representation from supplied string and construct the {@link Document} representation.
     * 
     * @param json the JSON representation; may not be null
     * @return the in-memory {@link Document} representation
     * @throws ParsingException if there was a problem reading from the stream
     */
    public Document read( String json ) throws ParsingException {
        return read(json, DEFAULT_INTROSPECT);
    }

    /**
     * Read the JSON representation from supplied input stream and construct the {@link Document} representation, using the
     * {@link Charset#defaultCharset() default character set}.
     * 
     * @param stream the input stream; may not be null
     * @param introspectStringValues true if the string values should be examined for common patterns, or false otherwise
     * @return the in-memory {@link Document} representation
     * @throws ParsingException if there was a problem reading from the stream
     */
    public Document read( InputStream stream,
                          boolean introspectStringValues ) throws ParsingException {
        return read(new InputStreamReader(stream), introspectStringValues);
    }

    /**
     * Read the JSON representation from supplied input stream and construct the {@link Document} representation, using the
     * supplied {@link Charset character set}.
     * 
     * @param stream the input stream; may not be null
     * @param charset the character set that should be used; may not be null
     * @param introspectStringValues true if the string values should be examined for common patterns, or false otherwise
     * @return the in-memory {@link Document} representation
     * @throws ParsingException if there was a problem reading from the stream
     */
    public Document read( InputStream stream,
                          Charset charset,
                          boolean introspectStringValues ) throws ParsingException {
        return read(new InputStreamReader(stream, charset), introspectStringValues);
    }

    /**
     * Read the JSON representation from supplied input stream and construct the {@link Document} representation.
     * 
     * @param reader the IO reader; may not be null
     * @param introspectStringValues true if the string values should be examined for common patterns, or false otherwise
     * @return the in-memory {@link Document} representation
     * @throws ParsingException if there was a problem reading from the stream
     */
    public Document read( Reader reader,
                          boolean introspectStringValues ) throws ParsingException {
        // Create an object so that this reader is thread safe ...
        ValueMatcher matcher = introspectStringValues ? DATE_VALUE_MATCHER : SIMPLE_VALUE_MATCHER;
        return new Parser(new Tokenizer(reader), VALUE_FACTORY, matcher).parseDocument();
    }

    /**
     * Read the JSON representation from supplied string and construct the {@link Document} representation.
     * 
     * @param json the JSON representation; may not be null
     * @param introspectStringValues true if the string values should be examined for common patterns, or false otherwise
     * @return the in-memory {@link Document} representation
     * @throws ParsingException if there was a problem reading from the stream
     */
    public Document read( String json,
                          boolean introspectStringValues ) throws ParsingException {
        return read(new StringReader(json), introspectStringValues);
    }

    /**
     * Return a {@link DocumentSequence} that can be used to pull multiple documents from the stream.
     * 
     * @param stream the input stream; may not be null
     * @return the sequence that can be used to get one or more Document instances from a single input
     */
    public DocumentSequence readMultiple( InputStream stream ) {
        return readMultiple(stream, DEFAULT_INTROSPECT);
    }

    /**
     * Return a {@link DocumentSequence} that can be used to pull multiple documents from the stream.
     * 
     * @param stream the input stream; may not be null
     * @param introspectStringValues true if the string values should be examined for common patterns, or false otherwise
     * @return the sequence that can be used to get one or more Document instances from a single input
     */
    public DocumentSequence readMultiple( InputStream stream,
                                          boolean introspectStringValues ) {
        return readMultiple(new InputStreamReader(stream), introspectStringValues);
    }

    /**
     * Return a {@link DocumentSequence} that can be used to pull multiple documents from the stream.
     * 
     * @param reader the IO reader; may not be null
     * @return the sequence that can be used to get one or more Document instances from a single input
     */
    public DocumentSequence readMultiple( Reader reader ) {
        return readMultiple(reader, DEFAULT_INTROSPECT);
    }

    /**
     * Return a {@link DocumentSequence} that can be used to pull multiple documents from the stream.
     * 
     * @param reader the IO reader; may not be null
     * @param introspectStringValues true if the string values should be examined for common patterns, or false otherwise
     * @return the sequence that can be used to get one or more Document instances from a single input
     */
    public DocumentSequence readMultiple( Reader reader,
                                          boolean introspectStringValues ) {
        // Create an object so that this reader is thread safe ...
        final Tokenizer tokenizer = new Tokenizer(reader);
        ValueMatcher matcher = introspectStringValues ? DATE_VALUE_MATCHER : SIMPLE_VALUE_MATCHER;
        final Parser parser = new Parser(tokenizer, VALUE_FACTORY, matcher);
        return new DocumentSequence() {
            @Override
            public Document nextDocument() throws ParsingException {
                if (tokenizer.isFinished()) return null;
                return parser.parseDocument(false);
            }
        };
    }

    /**
     * Parse the number represented by the supplied (unquoted) JSON field value.
     * 
     * @param value the string representation of the value
     * @return the number, or null if the value could not be parsed
     */
    public static Number parseNumber( String value ) {
        // Try to parse as a number ...
        char c = value.charAt(0);

        if ((c >= '0' && c <= '9') || c == '.' || c == '-' || c == '+') {
            // It's definitely a number ...
            if (c == '0' && value.length() > 2) {
                // it might be a hex number that starts with '0x'
                char two = value.charAt(1);
                if (two == 'x' || two == 'X') {
                    try {
                        // Parse the remainder of the hex number ...
                        int integer = Integer.parseInt(value.substring(2), 16);
                        return new Integer(integer);
                    } catch (NumberFormatException e) {
                        // Ignore and continue ...
                    }
                }
            }
            // Try parsing as a double ...
            try {
                if ((value.indexOf('.') > -1) || (value.indexOf('E') > -1) || (value.indexOf('e') > -1)) {
                    return Double.parseDouble(value);
                }
                Long longObj = new Long(value);
                long longValue = longObj.longValue();
                int intValue = longObj.intValue();
                if (longValue == intValue) {
                    // Then it's just an integer ...
                    return new Integer(intValue);
                }
                return longObj;
            } catch (NumberFormatException e) {
                // ignore ...
            }
        }
        return null;
    }

    /**
     * The component that parses a tokenized JSON stream.
     * 
     * @author Randall Hauch <rhauch@redhat.com> (C) 2011 Red Hat Inc.
     * @since 5.1
     */
    @NotThreadSafe
    public static class Parser {

        private final DocumentValueFactory values;
        private final Tokenizer tokens;
        private final ValueMatcher valueMatcher;

        /**
         * Create a new JsonReader that uses the supplied {@link Tokenizer} instance.
         * 
         * @param tokenizer the tokenizer that tokenizes the stream of JSON content; may not be null
         * @param values the factory for creating value objects; may not be null
         * @param valueMatcher the component that looks for patterns within string values to create alternative objects; may not
         *        be null
         */
        public Parser( Tokenizer tokenizer,
                       DocumentValueFactory values,
                       ValueMatcher valueMatcher ) {
            this.tokens = tokenizer;
            this.values = values;
            this.valueMatcher = valueMatcher;
        }

        /**
         * Parse the stream for the next JSON document.
         * 
         * @return the document, or null if there are no more documents
         * @throws ParsingException if there is a problem parsing the value
         */
        public Document parseDocument() throws ParsingException {
            return parseDocument(null, true);
        }

        /**
         * Parse the stream for the next JSON document.
         * 
         * @param failIfNotValidDocument true if this method should throw an exception if the stream does not contain a valid
         *        document, or false if null should be returned if there is no valid document on the stream
         * @return the document, or null if there are no more documents
         * @throws ParsingException if there is a problem parsing the value
         */
        public Document parseDocument( boolean failIfNotValidDocument ) throws ParsingException {
            return parseDocument(null, failIfNotValidDocument);
        }

        protected BasicDocument newDocument() {
            return new BasicDocument();
        }

        /**
         * Parse the stream for the next JSON document.
         * 
         * @param hasReservedFieldNames the flag that should be set if this document contains field names that are reserved
         * @param failIfNotValidDocument true if this method should throw an exception if the stream does not contain a valid
         *        document, or false if null should be returned if there is no valid document on the stream
         * @return the document, or null if there are no more documents
         * @throws ParsingException if there is a problem parsing the value
         */
        protected Document parseDocument( AtomicBoolean hasReservedFieldNames,
                                          boolean failIfNotValidDocument ) throws ParsingException {
            if (tokens.nextUsefulChar() != '{') {
                if (failIfNotValidDocument) {
                    throw tokens.error("JSON documents must begin with a '{' character");
                }
                // otherwise just return ...
                return null;
            }
            BasicDocument doc = newDocument();
            do {
                String fieldName = null;
                // Peek at the next character on the stream ...
                switch (tokens.peek()) {
                    case 0:
                        throw tokens.error("JSON documents must end with a '}' character");
                    case '}':
                        tokens.next();
                        return doc;
                    default:
                        // This should be a field name, so read it ...
                        fieldName = tokens.nextString();
                        break;
                }
                // Now look for any of the following delimieters: ':', "->", or "=>"
                tokens.nextFieldDelim();

                // Now look for a value ...
                Object value = parseValue();
                doc.put(fieldName, value);

                // Determine if this field is a reserved
                if (hasReservedFieldNames != null && isReservedFieldName(fieldName)) {
                    hasReservedFieldNames.set(true);
                }

                // Look for the delimiter between fields ...
                if (tokens.nextDocumentDelim()) return doc;
            } while (true);
        }

        protected final boolean isReservedFieldName( String fieldName ) {
            return fieldName.length() != 0 && fieldName.charAt(0) == '$';
        }

        /**
         * Parse the JSON array on the stream, beginning with the '[' character until the ']' character, which is consumed.
         * 
         * @return the array representation; never null but possibly an empty array
         * @throws ParsingException if there is a problem parsing the value
         */
        public BasicArray parseArray() throws ParsingException {
            if (tokens.nextUsefulChar() != '[') {
                throw tokens.error("JSON arrays must begin with a '[' character");
            }
            BasicArray array = new BasicArray();
            do {
                // Peek at the next character on the stream ...
                char c = tokens.peek();
                switch (c) {
                    case 0:
                        throw tokens.error("JSON arrays must end with a ']' character");
                    case ']':
                        tokens.next();
                        return array;
                    case ',':
                        tokens.next();
                        break;
                    default:
                        // This should be a value ..
                        Object value = parseValue();
                        array.addValue(value);
                        break;
                }
            } while (true);
        }

        /**
         * Parse the stream for the next field value, which can be one of the following values:
         * <ul>
         * <li>a nested document</li>
         * <li>an array of values</li>
         * <li>a string literal, surrounded by single-quote characters</li>
         * <li>a string literal, surrounded by double-quote characters</li>
         * <li>a string literal date of the form <code>"<i>yyyy</i>-<i>MM</i>-<i>dd</i>T<i>HH</i>:<i>mm</i>:<i>ss</i>"</code>
         * where <code>T</code> is a literal character</li>
         * <li>a string literal date of the form <code>"<i>yyyy</i>-<i>MM</i>-<i>dd</i>T<i>HH</i>:<i>mm</i>:<i>ss</i>Z"</code>
         * where <code>T</code> and <code>Z</code> are literal characters</li>
         * <li>a string literal date of the form
         * <code>"<i>yyyy</i>-<i>MM</i>-<i>dd</i>T<i>HH</i>:<i>mm</i>:<i>ss</i>GMT+<i>00</i>:<i>00</i>"</code> where
         * <code>T</code>, and <code>GMT</code> are literal characters</li>
         * <li>a string literal date of the form <code>"/Date(<i>millisOrIso</i>)/"</code></li>
         * <li>a string literal date of the form <code>"\/Date(<i>millisOrIso</i>)\/"</code></li>
         * <li>a date literal of the form <code>new Date(<i>millisOrIso</i>)</code></li>
         * <li>a date literal of the form <code>Date(<i>millisOrIso</i>)</code></li>
         * <li>a function of the form <code>new <i>functionName</i>(<i>parameters</i>)</code> where <code><i>parameters</i></code>
         * consists of one or more values as parsed by this method
         * </ul>
         * Note that in the date forms listed above, <code><i>millisOrIso</i></code> is either a long value representing the
         * number of milliseconds since epoch or a string literal in ISO-8601 format representing a date and time.
         * 
         * @return the field value
         * @throws ParsingException if there is a problem parsing the value
         */
        public Object parseValue() throws ParsingException {
            char c = tokens.peek();
            switch (c) {
                case 0:
                    // There's nothing left ...
                    return null;
                case '{':
                    // Nested object ...
                    AtomicBoolean hasReservedFieldNames = new AtomicBoolean();
                    Document doc = parseDocument(hasReservedFieldNames, true);
                    if (!hasReservedFieldNames.get()) {
                        return doc;
                    }
                    // Convert the doc with reserved field names ...
                    return processDocumentWithReservedFieldNames(doc);
                case '[':
                    // Nested array ...
                    return parseArray();
                case '"':
                case '\'':
                    String literal = tokens.nextString();
                    Object value = valueMatcher.parseValue(literal);
                    return value != null ? value : literal;
                case 'd':
                case 'n':
                    String newToken = tokens.nextWord(); // read the 'new' token
                    if ("new".equalsIgnoreCase(newToken) || "date".equalsIgnoreCase(newToken)) {
                        return parseFunction();
                    }
                    break;
            }

            // Looks like it's a number, so try that ...
            String number = tokens.nextNumber();
            return number != null ? parseValue(number, tokens.lineNumber(), tokens.columnNumber()) : number;
        }

        /**
         * Parse the value given by the supplied string located at the supplied line and column numbers. This method looks for
         * known constant values, then attempts to parse the value as a number, and then calls
         * {@link #parseUnknownValue(String, int, int)}.
         * 
         * @param value the string representation of the value
         * @param lineNumber the line number for the beginning of the value
         * @param columnNumber the column number for the beginning of the value
         * @return the value
         * @throws ParsingException if there is a problem parsing the value
         */
        public Object parseValue( String value,
                                  int lineNumber,
                                  int columnNumber ) throws ParsingException {
            if (value.length() == 0) return value;
            if ("true".equalsIgnoreCase(value)) return Boolean.TRUE;
            if ("false".equalsIgnoreCase(value)) return Boolean.FALSE;
            if ("null".equalsIgnoreCase(value)) return Null.getInstance();

            // Try to parse as a number ...
            Number number = parseNumber(value);
            if (number != null) return number;

            return parseUnknownValue(value, lineNumber, columnNumber);
        }

        /**
         * Parse the number represented by the supplied value. This method is called by the {@link #parseValue(String, int, int)}
         * method.
         * 
         * @param value the string representation of the value
         * @return the number, or null if the value could not be parsed
         */
        protected Number parseNumber( String value ) {
            return JsonReader.parseNumber(value);
        }

        /**
         * Override this method if custom value types are expected.
         * 
         * @param value the string representation of the value
         * @param lineNumber the line number at which the value starts
         * @param columnNumber the column number at which the value starts
         * @return the value
         * @throws ParsingException if there is a problem parsing the value
         */
        protected Object parseUnknownValue( String value,
                                            int lineNumber,
                                            int columnNumber ) throws ParsingException {
            return value;
        }

        /**
         * Parse a function call on the stream. The 'new' keyword has already been processed.
         * 
         * @return the result of the evaluation of the function
         * @throws ParsingException if there is a problem parsing the value
         */
        public Object parseFunction() throws ParsingException {
            // Parse the function name ...
            int line = tokens.lineNumber();
            int col = tokens.columnNumber();
            String functionName = tokens.nextString();
            FunctionCall function = new FunctionCall(functionName, line, col);

            // Read the open parenthesis ..
            char c = tokens.nextUsefulChar();
            if (c != '(') {
                throw tokens.error("Expected '(' after function name \"" + functionName + "\" and at line " + tokens.lineNumber()
                                   + ", column " + tokens.columnNumber());
            }

            // Read the parameters ...
            do {
                line = tokens.lineNumber();
                col = tokens.columnNumber();
                Object parameter = parseValue();
                if (parameter == null) {
                    break;
                }
                function.add(parameter, line, col);
            } while (true);

            // Now evaluate the function ...
            Object value = evaluateFunction(function);
            return value != null ? value : evaluateUnknownFunction(function);
        }

        /**
         * Method that is called to evaluate the supplied function. This method may be overridden by subclasses to handle custom
         * functions.
         * 
         * @param function the function definition
         * @return the value that resulted from evaluating the function, or null if the function call could not be evaluated
         * @throws ParsingException if there is a problem parsing the value
         */
        public Object evaluateFunction( FunctionCall function ) throws ParsingException {
            int numParams = function.size();
            if ("date".equalsIgnoreCase(function.getFunctionName())) {
                if (numParams > 0) {
                    // The parameter should be a long or a timestamp ...
                    FunctionParameter param1 = function.get(0);
                    Object value = param1.getValue();
                    if (value instanceof Long) {
                        Long millis = (Long)value;
                        return values.createDate(millis.longValue());
                    }
                    if (value instanceof Integer) {
                        Integer millis = (Integer)value;
                        return values.createDate(millis.longValue());
                    }
                    if (value instanceof String) {
                        String valueStr = (String)value;
                        try {
                            return values.createDate(valueStr);
                        } catch (ParseException e) {
                            // Not a valid date ...
                            throw tokens.error("Expecting the \"new Date(...)\" parameter to be a valid number of milliseconds or ISO date string, but found \""
                                               + param1.getValue()
                                               + "\" at line "
                                               + param1.getLineNumber()
                                               + ", column "
                                               + param1.getColumnNumber());
                        }
                    }
                }
                // Not a valid date ...
                throw tokens.error("The date function requires one parameter at line " + function.getLineNumber() + ", column "
                                   + function.getColumnNumber());
            }
            return null;
        }

        /**
         * Method that is called when the function call described by the parameter could not be evaluated. By default, the string
         * representation of the function is returned.
         * 
         * @param function the function definition
         * @return the value that resulted from evaluating the function
         * @throws ParsingException if there is a problem parsing the value
         */
        protected Object evaluateUnknownFunction( FunctionCall function ) throws ParsingException {
            return function.toString();
        }

        @SuppressWarnings( "deprecation" )
        protected Object processDocumentWithReservedFieldNames( Document doc ) {
            if (doc == null) return null;
            Object value = null;
            int numFields = doc.size();
            if (numFields == 0) return doc;
            try {
                if (numFields == 1) {
                    if (!Null.matches(value = doc.get(OBJECT_ID))) {
                        String bytesInBase16 = value.toString();
                        return values.createObjectId(bytesInBase16);
                    }
                    if (!Null.matches(value = doc.get(DATE))) {
                        if (value instanceof Date) {
                            return value;
                        }
                        String isoDate = value.toString();
                        try {
                            return values.createDate(isoDate);
                        } catch (ParseException e) {
                            Long millis = Long.parseLong(isoDate);
                            return values.createDate(millis);
                        }
                    }
                    if (!Null.matches(value = doc.get(REGEX_PATTERN))) {
                        String pattern = value.toString();
                        return values.createRegex(pattern, null);
                    }
                    if (!Null.matches(value = doc.get(UUID))) {
                        return values.createUuid(value.toString());
                    }
                    if (!Null.matches(value = doc.get(CODE))) {
                        String code = value.toString();
                        return values.createCode(code);
                    }
                } else if (numFields == 2) {
                    if (!Null.matches(value = doc.get(TIMESTAMP))) {
                        int time = doc.getInteger(TIMESTAMP);
                        int inc = doc.getInteger(INCREMENT);
                        return values.createTimestamp(time, inc);
                    }
                    if (!Null.matches(value = doc.get(REGEX_PATTERN))) {
                        String pattern = value.toString();
                        String options = doc.getString(REGEX_OPTIONS);
                        return values.createRegex(pattern, options);
                    }
                    if (!Null.matches(value = doc.get(CODE))) {
                        String code = value.toString();
                        Document scope = doc.getDocument(SCOPE);
                        return scope != null ? values.createCode(code, scope) : values.createCode(code);
                    }
                    if (!Null.matches(value = doc.get(BINARY_TYPE))) {
                        char c = value.toString().charAt(0);
                        byte type = 0x00;
                        switch (c) {
                            case '0':
                                type = BinaryType.GENERAL;
                                break;
                            case '1':
                                type = BinaryType.FUNCTION;
                                break;
                            case '2':
                                type = BinaryType.BINARY;
                                break;
                            case '3':
                                type = BinaryType.UUID;
                                break;
                            case '5':
                                type = BinaryType.MD5;
                                break;
                            case '8':
                                c = value.toString().charAt(1);
                                if (c == '0') {
                                    type = BinaryType.USER_DEFINED;
                                }
                                break;
                        }
                        String data = doc.getString(BASE_64);
                        return values.createBinary(type, Base64.decode(data));
                    }
                }
            } catch (Throwable e) {
                // ignore
            }
            return doc;
        }

        protected static class FunctionCall implements Iterable<FunctionParameter> {
            private final String functionName;
            private final List<FunctionParameter> parameters = new LinkedList<FunctionParameter>();
            private final int lineNumber;
            private final int columnNumber;

            public FunctionCall( String functionName,
                                 int lineNumber,
                                 int columnNumber ) {
                this.functionName = functionName;
                this.lineNumber = lineNumber;
                this.columnNumber = columnNumber;
            }

            public String getFunctionName() {
                return functionName;
            }

            public void add( Object parameter,
                             int lineNumber,
                             int columnNumber ) {
                this.parameters.add(new FunctionParameter(parameter, lineNumber, columnNumber));
            }

            @Override
            public Iterator<FunctionParameter> iterator() {
                return parameters.iterator();
            }

            public FunctionParameter get( int index ) {
                return parameters.get(index);
            }

            public int size() {
                return parameters.size();
            }

            public int getLineNumber() {
                return lineNumber;
            }

            public int getColumnNumber() {
                return columnNumber;
            }

            @Override
            public String toString() {
                StringBuilder sb = new StringBuilder(functionName);
                sb.append('(');
                boolean first = true;
                for (FunctionParameter parameter : parameters) {
                    if (first) {
                        first = false;
                    } else {
                        sb.append(',');
                    }
                    sb.append(parameter.getValue());
                }
                sb.append(')');
                return sb.toString();
            }
        }

        @Immutable
        protected static class FunctionParameter {
            private final Object value;
            private final int lineNumber;
            private final int columnNumber;

            public FunctionParameter( Object value,
                                      int lineNumber,
                                      int columnNumber ) {
                this.value = value;
                this.lineNumber = lineNumber;
                this.columnNumber = columnNumber;
            }

            public Object getValue() {
                return value;
            }

            public int getLineNumber() {
                return lineNumber;
            }

            public int getColumnNumber() {
                return columnNumber;
            }

            @Override
            public String toString() {
                return Json.write(value);
            }
        }
    }

    /**
     * The component that matches a string value for certain patterns. If the value matches a known pattern, it return the
     * appropriate value object; otherwise, the supplied string value is returned.
     * 
     * @author Randall Hauch <rhauch@redhat.com> (C) 2011 Red Hat Inc.
     * @since 5.1
     */
    @NotThreadSafe
    public static interface ValueMatcher {

        /**
         * Parse the value given by the supplied string into an appropriate value object. This method looks for specific patterns
         * of Date strings; if no known pattern is found, it just returns the supplied value.
         * 
         * @param value the string representation of the value
         * @return the value
         */
        public Object parseValue( String value );
    }

    /**
     * The component that matches a string value for certain patterns. If the value matches a known pattern, it return the
     * appropriate value object; otherwise, the supplied string value is returned.
     * 
     * @author Randall Hauch <rhauch@redhat.com> (C) 2011 Red Hat Inc.
     * @since 5.1
     */
    @NotThreadSafe
    public static class SimpleValueMatcher implements ValueMatcher {

        protected final DocumentValueFactory values;

        /**
         * Create a new matcher that uses the supplied {@link DocumentValueFactory} instance.
         * 
         * @param values the factory for creating value objects; may not be null
         */
        public SimpleValueMatcher( DocumentValueFactory values ) {
            this.values = values;
        }

        /**
         * Parse the value given by the supplied string into an appropriate value object. This method looks for specific patterns
         * of Date strings; if no known pattern is found, it just returns the supplied value.
         * 
         * @param value the string representation of the value
         * @return the value
         */
        @Override
        public Object parseValue( String value ) {
            return value;
        }
    }

    /**
     * The component that parses a tokenized JSON stream and attempts to evaluate literal values such as dates
     * 
     * @author Randall Hauch <rhauch@redhat.com> (C) 2011 Red Hat Inc.
     * @since 5.1
     */
    @NotThreadSafe
    public static class DateValueMatcher extends SimpleValueMatcher {

        /**
         * Create a new matcher that uses the supplied {@link DocumentValueFactory} instance.
         * 
         * @param values the factory for creating value objects; may not be null
         */
        public DateValueMatcher( DocumentValueFactory values ) {
            super(values);
        }

        /**
         * Parse the value given by the supplied string into an appropriate value object. This method looks for specific patterns
         * of Date strings; if no known pattern is found, it just returns the supplied value.
         * 
         * @param value the string representation of the value
         * @return the value
         */
        @Override
        public Object parseValue( String value ) {
            if (value != null && value.length() > 2) {
                Date date = parseDateFromLiteral(value);
                if (date != null) {
                    return date;
                }
            }
            return value;
        }

        /**
         * Parse the date represented by the supplied value. This method is called by the {@link #parseValue(String)} method. This
         * method checks the following formats:
         * <ul>
         * <li>a string literal date of the form <code>"<i>yyyy</i>-<i>MM</i>-<i>dd</i>T<i>HH</i>:<i>mm</i>:<i>ss</i>"</code>
         * where <code>T</code> is a literal character</li>
         * <li>a string literal date of the form <code>"<i>yyyy</i>-<i>MM</i>-<i>dd</i>T<i>HH</i>:<i>mm</i>:<i>ss</i>Z"</code>
         * where <code>T</code> and <code>Z</code> are literal characters</li>
         * <li>a string literal date of the form
         * <code>"<i>yyyy</i>-<i>MM</i>-<i>dd</i>T<i>HH</i>:<i>mm</i>:<i>ss</i>GMT+<i>00</i>:<i>00</i>"</code> where
         * <code>T</code>, and <code>GMT</code> are literal characters</li>
         * <li>a string literal date of the form <code>"/Date(<i>millisOrIso</i>)/"</code></li>
         * <li>a string literal date of the form <code>"\/Date(<i>millisOrIso</i>)\/"</code></li>
         * </ul>
         * <p>
         * Note that this method does not handle the <code>new Date(...)</code> or <code>Date(...)</code> representations, as
         * that's handled elsewhere.
         * </p>
         * 
         * @param value the string representation of the value; never null and never empty
         * @return the number, or null if the value could not be parsed
         */
        protected Date parseDateFromLiteral( String value ) {
            char f = value.charAt(0);
            if (Character.isDigit(f)) {
                // Try as simply an ISO-8601 formatted date ...
                return evaluateDate(value);
            }
            if (value.startsWith("\\/Date(") && value.endsWith(")\\/")) {
                String millisOrIso = value.substring(7, value.length() - 3).trim();
                return evaluateDate(millisOrIso);
            }
            if (value.startsWith("/Date(") && value.endsWith(")/")) {
                String millisOrIso = value.substring(6, value.length() - 2).trim();
                return evaluateDate(millisOrIso);
            }
            return null;
        }

        protected Date evaluateDate( String millisOrIso ) {
            try {
                return values.createDate(millisOrIso);
            } catch (ParseException e) {
                // not an ISO-8601 format ...
            }
            return null;
        }

    }

    /**
     * The component that tokenizes a stream of JSON content.
     * 
     * @author Randall Hauch <rhauch@redhat.com> (C) 2011 Red Hat Inc.
     * @since 5.1
     */
    @NotThreadSafe
    public static class Tokenizer {

        private final Reader reader;
        private int lineNumber;
        private int columnNumber;
        private boolean finished;
        private boolean hasPrevious;
        private char previous;
        private StringBuilder stringBuilder = new StringBuilder(128);

        /**
         * Create a new tokenizer that uses the supplied {@link Reader Java IO Reader} instance.
         * 
         * @param reader the reader for accessing the JSON content; may not be null
         */
        public Tokenizer( Reader reader ) {
            this.reader = reader;
        }

        public boolean isFinished() {
            return finished;
        }

        protected char next() throws ParsingException {
            char c = 0;
            if (hasPrevious) {
                hasPrevious = false;
                c = previous;
            } else {
                try {
                    int x = reader.read();
                    if (x <= 0) {
                        // We've reached the end of the stream ...
                        finished = true;
                        c = 0;
                    } else {
                        c = (char)x;
                    }
                } catch (IOException e) {
                    throw error("Error reading at line " + lineNumber + ", column " + columnNumber + ": "
                                + e.getLocalizedMessage(),
                                e);
                }
            }
            // It's a valid character, but we have to advance our line & column counts ...
            if (previous == '\r') {
                ++lineNumber;
                columnNumber = (c == '\n' ? 0 : 1);
            } else if (c == '\n') {
                ++lineNumber;
                columnNumber = 0;
            } else {
                ++columnNumber;
            }
            previous = c;
            return c;
        }

        public String next( int characterCount ) throws ParsingException {
            StringBuilder sb = stringBuilder();
            for (int i = 0; i != characterCount; ++i) {
                sb.append(next());
            }
            return complete(sb);
        }

        protected final StringBuilder stringBuilder() {
            StringBuilder stringBuilder = this.stringBuilder != null ? this.stringBuilder : new StringBuilder(128);
            this.stringBuilder = null;
            stringBuilder.delete(0, stringBuilder.length());
            return stringBuilder;
        }

        protected final String complete( StringBuilder sb ) {
            assert sb != null;
            assert stringBuilder == null;
            stringBuilder = sb;
            return sb.toString();
        }

        public char nextUsefulChar() throws ParsingException {
            do {
                char next = next();
                if (next == 0 || (next != ' ' && next != '\t' && next != '\n' && next != '\r')) return next;
            } while (true);
        }

        public char peek() throws ParsingException {
            if (hasPrevious) {
                return previous;
            }
            char next = nextUsefulChar();
            hasPrevious = true;
            previous = next;
            --columnNumber;
            return next;
        }

        /**
         * Read the next quoted string from the stream, where stream begins with the a single-quote or double-quote character and
         * the string ends with the same quote character.
         * 
         * @return the next string; never null
         * @throws ParsingException
         */
        public String nextString() throws ParsingException {
            char c = nextUsefulChar();
            switch (c) {
                case '"':
                case '\'':
                    return nextString(c);
            }
            throw error("Expecting a field name at line " + lineNumber + ", column " + columnNumber
                        + ". Check for a missing comma.");
        }

        public String nextString( char endQuote ) throws ParsingException {
            StringBuilder sb = stringBuilder();
            char c = 0;
            do {
                c = next();
                switch (c) {
                    case 0:
                    case '\n':
                    case '\r':
                        // The string was not properly terminated ...
                        throw error("The string was not terminated before the end of line or end of document, at line "
                                    + lineNumber + ", column " + columnNumber);
                    case '\\':
                        // Escape sequence ...
                        c = next();
                        switch (c) {
                            case '\'': // single quote
                            case '"': // double quote
                            case '\\': // reverse solidus
                            case '/': // forward solidus
                                break;
                            case 'b':
                                c = '\b';
                                break;
                            case 'f':
                                c = '\f';
                                break;
                            case 'n':
                                c = '\n';
                                break;
                            case 'r':
                                c = '\r';
                                break;
                            case 't':
                                c = '\t';
                                break;
                            case 'u':
                                // Unicode sequence made of exactly 4 hex characters ...
                                char[] hex = new char[4];
                                hex[0] = next();
                                hex[1] = next();
                                hex[2] = next();
                                hex[3] = next();
                                String code = new String(hex, 0, 4);
                                try {
                                    c = (char)Integer.parseInt(code, 16); // hex
                                } catch (NumberFormatException e) {
                                    columnNumber -= 6;
                                    throw error("Expecting escaped unicode sequence of hex characters but found '\\u" + code
                                                + "' at line " + lineNumber + ", column " + columnNumber);
                                }
                                break;
                            default:
                                // No other characters are valid escaped sequences, so this is actually just a backslash
                                // followed by the current character c. So append the backslash ...
                                sb.append('\\');
                                // then the character ...
                                break;
                        }
                        sb.append(c);
                        break;
                    default:
                        // Just a regular character (or the end quote) ...
                        if (c == endQuote) {
                            // This is the only way to successfully exit this method!
                            return complete(sb);
                        }
                        // just a regular character ...
                        sb.append(c);
                }
            } while (true);
        }

        public void nextFieldDelim() throws ParsingException {
            try {
                switch (nextUsefulChar()) {
                    case ':':
                    case '=':
                        if (peek() == '>') {
                            next(); // consume the '>'
                        }
                        break;
                }
            } catch (ParsingException e) {
                throw error("Expecting a field delimiter (either ':', '=' or '=>') at line " + lineNumber + ", column "
                            + columnNumber);
            }
        }

        /**
         * Consume the next document delimiter (either a ',' or a ';'), and return whether the end-of-document character (e.g.,
         * '}') has been consumed. This will correctly handle repeated delimiters, which are technically incorrect.
         * 
         * @return true if a '}' has been consumed, or false otherwise
         * @throws ParsingException if the document delimiter could not be read
         */
        public boolean nextDocumentDelim() throws ParsingException {
            switch (nextUsefulChar()) {
                case ';': // handle ';' delimiters, too!
                case ',':
                    switch (peek()) {
                        case ':':
                        case ',':
                            // There are multiple delimiters in a row. Strictly speaking, this is invalid but we
                            // can easily handle it anyway ...
                            return nextDocumentDelim();
                        case '}':
                            // The comma was before '}' - this is not strictly well-formed, but we'll handle it
                            next();
                            return true;
                    }
                    return false;
                case '}':
                    return true;
            }
            return false;
        }

        /**
         * Return a string containing the next number on the stream.
         * 
         * @return the next number as a string, or null if there is no content on the stream
         * @throws ParsingException if the number could not be read
         */
        public String nextNumber() throws ParsingException {
            char c = peek();
            if (c == 0) {
                return null;
            }
            StringBuilder sb = stringBuilder();
            while (c > ' ' && "{}[]:\"=#/\\',;".indexOf(c) <= -1) {
                if (c == 0) {
                    break;
                }
                sb.append(next());
                c = peek();
            }
            return complete(sb);
        }

        /**
         * Return a string containing the next alpha-numeric word on the stream.
         * 
         * @return the next word as a string
         * @throws ParsingException if the number could not be read
         */
        public String nextWord() throws ParsingException {
            char c = peek();
            StringBuilder sb = stringBuilder();
            while (Character.isLetterOrDigit(c)) {
                sb.append(next());
                c = peek();
            }
            return complete(sb);
        }

        public ParsingException error( String message ) {
            return new ParsingException(message, lineNumber, columnNumber);
        }

        public ParsingException error( String message,
                                       Throwable t ) {
            return new ParsingException(message, t, lineNumber, columnNumber);
        }

        public int lineNumber() {
            return lineNumber;
        }

        public int columnNumber() {
            return columnNumber;
        }
    }
}
