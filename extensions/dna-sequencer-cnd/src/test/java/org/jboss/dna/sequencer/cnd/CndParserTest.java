/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors. 
 *
 * JBoss DNA is free software. Unless otherwise indicated, all code in JBoss DNA
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * JBoss DNA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.dna.sequencer.cnd;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.antlr.runtime.ANTLRInputStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RuleReturnScope;
import org.antlr.runtime.tree.RewriteEmptyStreamException;
import org.junit.After;
import org.junit.Test;

public class CndParserTest {

    // =============================================================================================================================
    // Fields
    // =============================================================================================================================

    private InputStream stream;

    // =============================================================================================================================
    // Methods
    // =============================================================================================================================

    @After
    public void afterEach() throws IOException {
        if (this.stream != null) {
            try {
                this.stream.close();
            } finally {
                this.stream = null;
            }
        }
    }

    private CndParser createParser( String input ) throws IOException {
        afterEach(); // make sure previous stream is closed
        this.stream = new ByteArrayInputStream(input.getBytes("UTF-8"));
        CndLexer lexer = new CndLexer(new ANTLRInputStream(this.stream));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        return new CndParser(tokens);
    }

    // =============================================================================================================================
    // Comment Parsing Tests
    // =============================================================================================================================

    private void parseComment( String option,
                               boolean success ) throws Exception {
        CndParser parser = createParser(option);
        RuleReturnScope scope = parser.comment();

        if (success) {
            assertThat("Value '" + option + "' did not parse and should have", scope.getTree(), notNullValue());
            assertThat(option, is(((org.antlr.runtime.tree.CommonTree)scope.getTree()).getText()));
        } else {
            assertThat("Value '" + option + "' should not parse and did", scope.getTree(), nullValue());
        }
    }

    @Test
    public void shouldParseSingleLineComment() throws Exception {
        parseComment("// This is a single line comment", true);
    }

    @Test
    public void shouldParseMultiLineComment() throws Exception {
        parseComment("/* This\nis\na\nmulti-line\ncomment */", true);
    }

    @Test
    public void shouldNotParseNonComment() throws Exception {
        parseComment("This is not a valid comment", false);
    }

    @Test
    public void shouldNotParseMalformedComment() throws Exception {
        parseComment("/* This comment is missing ending delimeters", false);
    }

    // =============================================================================================================================
    // Node Type Name Parsing Tests
    // =============================================================================================================================

    private void parseNodeTypeName( String name,
                                    boolean success ) throws Exception {
        CndParser parser = createParser(name);
        RuleReturnScope scope = parser.node_type_name();

        if (success) {
            assertThat("Value '" + name + "' did not parse and should have", scope.getTree(), notNullValue());

            // strip off brackets
            assertThat(name.substring(1, name.length() - 1), is(((org.antlr.runtime.tree.CommonTree)scope.getTree()).getText()));
        } else {
            assertThat("Value '" + name + "' should not parse", scope.getTree(), nullValue());
        }
    }

    @Test
    public void shouldParseUnquotedNodeTypeName() throws Exception {
        parseNodeTypeName("[typeName]", true);
    }

    @Test
    public void shouldParseQuotedNodeTypeName() throws Exception {
        parseNodeTypeName("['quotedName']", true);
    }

    @Test
    public void shouldNotParseNodeTypeNameWithSpaces() throws Exception {
        parseNodeTypeName("[no spaces allowed]", false);
    }

    @Test
    public void shouldNotParseNodeTypeNameWithMissingEndBracket() throws Exception {
        parseNodeTypeName("[missingClosingBracket", false);
    }

    @Test( expected = RewriteEmptyStreamException.class )
    public void shouldNotParseNodeTypeNameWithMissingBracketsButNoName() throws Exception {
        parseNodeTypeName("[]", false);
    }

    // =============================================================================================================================
    // Node Type Abstract Option Parsing Tests
    // =============================================================================================================================

    private void parseAbstractOption( String option,
                                      boolean success ) throws Exception {
        CndParser parser = createParser(option);
        RuleReturnScope scope = parser.abs_opt();

        if (success) {
            assertThat("Value '" + option + "' did not parse and should have", scope.getTree(), notNullValue());
            assertThat(option, is(((org.antlr.runtime.tree.CommonTree)scope.getTree()).getText()));
        } else {
            assertThat("Value '" + option + "' should not parse and did", scope.getTree(), nullValue());
        }
    }

    @Test
    public void shouldParseAbstractOption() throws Exception {
        parseAbstractOption("abstract", true);
        parseAbstractOption("abs", true);
        parseAbstractOption("a", true);
    }

    @Test
    public void shouldNotParseInvalidAbstractOption() throws Exception {
        parseAbstractOption("abstractoption", false);
        parseAbstractOption("A", false);
    }

    // =============================================================================================================================
    // Node Type Orderable Option Parsing Tests
    // =============================================================================================================================

    private void parseOrderableOption( String option,
                                       boolean success ) throws Exception {
        CndParser parser = createParser(option);
        RuleReturnScope scope = parser.orderable_opt();

        if (success) {
            assertThat("Value '" + option + "' did not parse and should have", scope.getTree(), notNullValue());
            assertThat(option, is(((org.antlr.runtime.tree.CommonTree)scope.getTree()).getText()));
        } else {
            assertThat("Value '" + option + "' should not parse and did", scope.getTree(), nullValue());
        }
    }

    @Test
    public void shouldParseOrderableOption() throws Exception {
        parseOrderableOption("orderable", true);
        parseOrderableOption("ord", true);
        parseOrderableOption("o", true);
    }

    @Test
    public void shouldNotParseInvalidOrderableOption() throws Exception {
        parseOrderableOption("orderableoption", false);
        parseOrderableOption("O", false);
    }

    // =============================================================================================================================
    // Node Type Mixin Option Parsing Tests
    // =============================================================================================================================

    private void parseMixinOption( String option,
                                   boolean success ) throws Exception {
        CndParser parser = createParser(option);
        RuleReturnScope scope = parser.mixin_opt();

        if (success) {
            assertThat("Value '" + option + "' did not parse and should have", scope.getTree(), notNullValue());
            assertThat(option, is(((org.antlr.runtime.tree.CommonTree)scope.getTree()).getText()));
        } else {
            assertThat("Value '" + option + "' should not parse and did", scope.getTree(), nullValue());
        }
    }

    @Test
    public void shouldParseMixinOption() throws Exception {
        parseMixinOption("mixin", true);
        parseMixinOption("mix", true);
        parseMixinOption("m", true);
    }

    @Test
    public void shouldNotParseInvalidMixinOption() throws Exception {
        parseMixinOption("mixinoption", false);
        parseMixinOption("M", false);
    }

    // =============================================================================================================================
    // Node Type Orderable Mixin Options Parsing Tests
    // =============================================================================================================================

    private void parseNodeTypeOrderableMixinOptions( String options,
                                                     boolean success,
                                                     int numChildren ) throws Exception {
        CndParser parser = createParser(options);
        RuleReturnScope scope = parser.ord_mix_opt();

        if (success) {
            assertThat("Value '" + options + "' did not parse and should have", scope.getTree(), notNullValue());

            if (numChildren == 0) {
                assertThat(options, is(((org.antlr.runtime.tree.CommonTree)scope.getTree()).getText()));
            } else {
                assertThat(numChildren, is(((org.antlr.runtime.tree.CommonTree)scope.getTree()).getChildCount()));
            }
        } else {
            assertThat("Value '" + options + "' should not parse and did", scope.getTree(), nullValue());
        }
    }

    @Test
    public void shouldParseNodeTypeOrderableMixinOptions() throws Exception {
        parseNodeTypeOrderableMixinOptions("orderable", true, 0);
        parseNodeTypeOrderableMixinOptions("orderable mixin", true, 2);
        parseNodeTypeOrderableMixinOptions("orderable mix", true, 2);
        parseNodeTypeOrderableMixinOptions("orderable m", true, 2);

        parseNodeTypeOrderableMixinOptions("ord", true, 0);
        parseNodeTypeOrderableMixinOptions("ord mixin", true, 2);
        parseNodeTypeOrderableMixinOptions("ord mix", true, 2);
        parseNodeTypeOrderableMixinOptions("ord m", true, 2);

        parseNodeTypeOrderableMixinOptions("o", true, 0);
        parseNodeTypeOrderableMixinOptions("o mixin", true, 2);
        parseNodeTypeOrderableMixinOptions("o mix", true, 2);
        parseNodeTypeOrderableMixinOptions("o m", true, 2);

        parseNodeTypeOrderableMixinOptions("mixin", true, 0);
        parseNodeTypeOrderableMixinOptions("mixin orderable", true, 2);
        parseNodeTypeOrderableMixinOptions("mixin ord", true, 2);
        parseNodeTypeOrderableMixinOptions("mixin o", true, 2);

        parseNodeTypeOrderableMixinOptions("mix", true, 0);
        parseNodeTypeOrderableMixinOptions("mix orderable", true, 2);
        parseNodeTypeOrderableMixinOptions("mix ord", true, 2);
        parseNodeTypeOrderableMixinOptions("mix o", true, 2);

        parseNodeTypeOrderableMixinOptions("m", true, 0);
        parseNodeTypeOrderableMixinOptions("m orderable", true, 2);
        parseNodeTypeOrderableMixinOptions("m ord", true, 2);
        parseNodeTypeOrderableMixinOptions("m o", true, 2);
    }

    // =============================================================================================================================
    // Node Type Options Parsing Tests
    // =============================================================================================================================

    private void parseNodeTypeOptions( String options,
                                       boolean success,
                                       int numChildren ) throws Exception {
        CndParser parser = createParser(options);
        RuleReturnScope scope = parser.node_type_options();

        if (success) {
            assertThat("Value '" + options + "' did not parse and should have", scope.getTree(), notNullValue());

            if (numChildren == 0) {
                assertThat(options, is(((org.antlr.runtime.tree.CommonTree)scope.getTree()).getText()));
            } else {
                assertThat(numChildren, is(((org.antlr.runtime.tree.CommonTree)scope.getTree()).getChildCount()));
            }
        } else {
            assertThat("Value '" + options + "' should not parse and did", scope.getTree(), nullValue());
        }
    }

    @Test
    public void shouldParseNodeTypeOptions() throws Exception {
        parseNodeTypeOptions("abstract", true, 0);
        parseNodeTypeOptions("abs", true, 0);
        parseNodeTypeOptions("a", true, 0);

        parseNodeTypeOptions("abstract orderable", true, 2);
        parseNodeTypeOptions("abstract ord", true, 2);
        parseNodeTypeOptions("abstract o", true, 2);

        parseNodeTypeOptions("abs orderable", true, 2);
        parseNodeTypeOptions("abs ord", true, 2);
        parseNodeTypeOptions("abs o", true, 2);

        parseNodeTypeOptions("a orderable", true, 2);
        parseNodeTypeOptions("a ord", true, 2);
        parseNodeTypeOptions("a o", true, 2);

        parseNodeTypeOptions("abstract mixin", true, 2);
        parseNodeTypeOptions("abstract mix", true, 2);
        parseNodeTypeOptions("abstract m", true, 2);

        parseNodeTypeOptions("abs mixin", true, 2);
        parseNodeTypeOptions("abs mix", true, 2);
        parseNodeTypeOptions("abs m", true, 2);

        parseNodeTypeOptions("a mixin", true, 2);
        parseNodeTypeOptions("a mix", true, 2);
        parseNodeTypeOptions("a m", true, 2);

        parseNodeTypeOptions("abstract orderable mixin", true, 3);
        parseNodeTypeOptions("abstract orderable mix", true, 3);
        parseNodeTypeOptions("abstract orderable m", true, 3);

        parseNodeTypeOptions("abstract ord mixin", true, 3);
        parseNodeTypeOptions("abstract ord mix", true, 3);
        parseNodeTypeOptions("abstract ord m", true, 3);

        parseNodeTypeOptions("abstract o mixin", true, 3);
        parseNodeTypeOptions("abstract o mix", true, 3);
        parseNodeTypeOptions("abstract o m", true, 3);

        parseNodeTypeOptions("abs orderable mixin", true, 3);
        parseNodeTypeOptions("abs orderable mix", true, 3);
        parseNodeTypeOptions("abs orderable m", true, 3);

        parseNodeTypeOptions("abs ord mixin", true, 3);
        parseNodeTypeOptions("abs ord mix", true, 3);
        parseNodeTypeOptions("abs ord m", true, 3);

        parseNodeTypeOptions("abs o mixin", true, 3);
        parseNodeTypeOptions("abs o mix", true, 3);
        parseNodeTypeOptions("abs o m", true, 3);

        parseNodeTypeOptions("a orderable mixin", true, 3);
        parseNodeTypeOptions("a orderable mix", true, 3);
        parseNodeTypeOptions("a orderable m", true, 3);

        parseNodeTypeOptions("a ord mixin", true, 3);
        parseNodeTypeOptions("a ord mix", true, 3);
        parseNodeTypeOptions("a ord m", true, 3);

        parseNodeTypeOptions("a o mixin", true, 3);
        parseNodeTypeOptions("a o mix", true, 3);
        parseNodeTypeOptions("a o m", true, 3);

        parseNodeTypeOptions("abstract mixin orderable", true, 3);
        parseNodeTypeOptions("abstract mixin ord", true, 3);
        parseNodeTypeOptions("abstract mixin o", true, 3);

        parseNodeTypeOptions("abstract mix orderable", true, 3);
        parseNodeTypeOptions("abstract mix ord", true, 3);
        parseNodeTypeOptions("abstract mix o", true, 3);

        parseNodeTypeOptions("abstract m orderable", true, 3);
        parseNodeTypeOptions("abstract m ord", true, 3);
        parseNodeTypeOptions("abstract m o", true, 3);

        parseNodeTypeOptions("abs mixin orderable", true, 3);
        parseNodeTypeOptions("abs mixin ord", true, 3);
        parseNodeTypeOptions("abs mixin o", true, 3);

        parseNodeTypeOptions("abs mix orderable", true, 3);
        parseNodeTypeOptions("abs mix ord", true, 3);
        parseNodeTypeOptions("abs mix o", true, 3);

        parseNodeTypeOptions("abs m orderable", true, 3);
        parseNodeTypeOptions("abs m ord", true, 3);
        parseNodeTypeOptions("abs m o", true, 3);

        parseNodeTypeOptions("a mixin orderable", true, 3);
        parseNodeTypeOptions("a mixin ord", true, 3);
        parseNodeTypeOptions("a mixin o", true, 3);

        parseNodeTypeOptions("a mix orderable", true, 3);
        parseNodeTypeOptions("a mix ord", true, 3);
        parseNodeTypeOptions("a mix o", true, 3);

        parseNodeTypeOptions("a m orderable", true, 3);
        parseNodeTypeOptions("a m ord", true, 3);
        parseNodeTypeOptions("a m o", true, 3);

        parseNodeTypeOptions("orderable abstract", true, 2);
        parseNodeTypeOptions("ord abstract", true, 2);
        parseNodeTypeOptions("o abstract", true, 2);

        parseNodeTypeOptions("orderable abs", true, 2);
        parseNodeTypeOptions("ord abs", true, 2);
        parseNodeTypeOptions("o abs", true, 2);

        parseNodeTypeOptions("orderable a", true, 2);
        parseNodeTypeOptions("ord a", true, 2);
        parseNodeTypeOptions("o a", true, 2);

        parseNodeTypeOptions("mixin abstract", true, 2);
        parseNodeTypeOptions("mix abstract", true, 2);
        parseNodeTypeOptions("m abstract", true, 2);

        parseNodeTypeOptions("mixin abs", true, 2);
        parseNodeTypeOptions("mix abs", true, 2);
        parseNodeTypeOptions("m abs", true, 2);

        parseNodeTypeOptions("mixin a", true, 2);
        parseNodeTypeOptions("mix a", true, 2);
        parseNodeTypeOptions("m a", true, 2);

        parseNodeTypeOptions("orderable mixin abstract", true, 3);
        parseNodeTypeOptions("orderable mix abstract", true, 3);
        parseNodeTypeOptions("orderable m abstract", true, 3);

        parseNodeTypeOptions("ord mixin abstract", true, 3);
        parseNodeTypeOptions("ord mix abstract", true, 3);
        parseNodeTypeOptions("ord m abstract", true, 3);

        parseNodeTypeOptions("o mixin abstract", true, 3);
        parseNodeTypeOptions("o mix abstract", true, 3);
        parseNodeTypeOptions("o m abstract", true, 3);

        parseNodeTypeOptions("orderable mixin abs", true, 3);
        parseNodeTypeOptions("orderable mix abs", true, 3);
        parseNodeTypeOptions("orderable m abs", true, 3);

        parseNodeTypeOptions("ord mixin abs", true, 3);
        parseNodeTypeOptions("ord mix abs", true, 3);
        parseNodeTypeOptions("ord m abs", true, 3);

        parseNodeTypeOptions("o mixin abs", true, 3);
        parseNodeTypeOptions("o mix abs", true, 3);
        parseNodeTypeOptions("o m abs", true, 3);

        parseNodeTypeOptions("orderable mixin a", true, 3);
        parseNodeTypeOptions("orderable mix a", true, 3);
        parseNodeTypeOptions("orderable m a", true, 3);

        parseNodeTypeOptions("ord mixin a", true, 3);
        parseNodeTypeOptions("ord mix a", true, 3);
        parseNodeTypeOptions("ord m a", true, 3);

        parseNodeTypeOptions("o mixin a", true, 3);
        parseNodeTypeOptions("o mix a", true, 3);
        parseNodeTypeOptions("o m a", true, 3);

        parseNodeTypeOptions("mixin orderable abstract", true, 3);
        parseNodeTypeOptions("mixin ord abstract", true, 3);
        parseNodeTypeOptions("mixin o abstract", true, 3);

        parseNodeTypeOptions("mix orderable abstract", true, 3);
        parseNodeTypeOptions("mix ord abstract", true, 3);
        parseNodeTypeOptions("mix o abstract", true, 3);

        parseNodeTypeOptions("m orderable abstract", true, 3);
        parseNodeTypeOptions("m ord abstract", true, 3);
        parseNodeTypeOptions("m o abstract", true, 3);

        parseNodeTypeOptions("mixin orderable abs", true, 3);
        parseNodeTypeOptions("mixin ord abs", true, 3);
        parseNodeTypeOptions("mixin o abs", true, 3);

        parseNodeTypeOptions("mix orderable abs", true, 3);
        parseNodeTypeOptions("mix ord abs", true, 3);
        parseNodeTypeOptions("mix o abs", true, 3);

        parseNodeTypeOptions("m orderable abs", true, 3);
        parseNodeTypeOptions("m ord abs", true, 3);
        parseNodeTypeOptions("m o abs", true, 3);

        parseNodeTypeOptions("mixin orderable a", true, 3);
        parseNodeTypeOptions("mixin ord a", true, 3);
        parseNodeTypeOptions("mixin o a", true, 3);

        parseNodeTypeOptions("mix orderable a", true, 3);
        parseNodeTypeOptions("mix ord a", true, 3);
        parseNodeTypeOptions("mix o a", true, 3);

        parseNodeTypeOptions("m orderable a", true, 3);
        parseNodeTypeOptions("m ord a", true, 3);
        parseNodeTypeOptions("m o a", true, 3);

        parseNodeTypeOptions("orderable", true, 0);
        parseNodeTypeOptions("ord", true, 0);
        parseNodeTypeOptions("o", true, 0);

        parseNodeTypeOptions("mixin", true, 0);
        parseNodeTypeOptions("mix", true, 0);
        parseNodeTypeOptions("m", true, 0);

        parseNodeTypeOptions("orderable mixin", true, 2);
        parseNodeTypeOptions("orderable mix", true, 2);
        parseNodeTypeOptions("orderable m", true, 2);

        parseNodeTypeOptions("ord mixin", true, 2);
        parseNodeTypeOptions("ord mix", true, 2);
        parseNodeTypeOptions("ord m", true, 2);

        parseNodeTypeOptions("o mixin", true, 2);
        parseNodeTypeOptions("o mix", true, 2);
        parseNodeTypeOptions("o m", true, 2);

        parseNodeTypeOptions("mixin orderable", true, 2);
        parseNodeTypeOptions("mixin ord", true, 2);
        parseNodeTypeOptions("mixin o", true, 2);

        parseNodeTypeOptions("mix orderable", true, 2);
        parseNodeTypeOptions("mix ord", true, 2);
        parseNodeTypeOptions("mix o", true, 2);

        parseNodeTypeOptions("m orderable", true, 2);
        parseNodeTypeOptions("m ord", true, 2);
        parseNodeTypeOptions("m o", true, 2);
    }

    // =============================================================================================================================
    // Property Type Parsing Tests
    // =============================================================================================================================

    private void parsePropertyType( String propertyType,
                                    boolean success ) throws Exception {
        CndParser parser = createParser(propertyType);
        RuleReturnScope scope = parser.property_type();

        if (success) {
            assertThat("Value '" + propertyType + "' did not parse and should have", scope.getTree(), notNullValue());
            assertThat(propertyType, is(((org.antlr.runtime.tree.CommonTree)scope.getTree()).getText()));
        } else {
            assertThat("Value '" + propertyType + "' should not parse and did", scope.getTree(), nullValue());
        }
    }

    @Test
    public void shouldParseStringPropertyType() throws Exception {
        parsePropertyType("STRING", true);
        parsePropertyType("String", true);
        parsePropertyType("string", true);
    }

    @Test
    public void shouldParseBinaryPropertyType() throws Exception {
        parsePropertyType("BINARY", true);
        parsePropertyType("Binary", true);
        parsePropertyType("binary", true);
    }

    @Test
    public void shouldParseLongPropertyType() throws Exception {
        parsePropertyType("LONG", true);
        parsePropertyType("Long", true);
        parsePropertyType("long", true);
    }

    @Test
    public void shouldParseDoublePropertyType() throws Exception {
        parsePropertyType("DOUBLE", true);
        parsePropertyType("Double", true);
        parsePropertyType("double", true);
    }

    @Test
    public void shouldParseDecimalPropertyType() throws Exception {
        parsePropertyType("DECIMAL", true);
        parsePropertyType("Decimal", true);
        parsePropertyType("decimal", true);
    }

    @Test
    public void shouldParseBooleanPropertyType() throws Exception {
        parsePropertyType("BOOLEAN", true);
        parsePropertyType("Boolean", true);
        parsePropertyType("boolean", true);
    }

    @Test
    public void shouldParseDatePropertyType() throws Exception {
        parsePropertyType("DATE", true);
        parsePropertyType("Date", true);
        parsePropertyType("date", true);
    }

    @Test
    public void shouldParseNamePropertyType() throws Exception {
        parsePropertyType("NAME", true);
        parsePropertyType("Name", true);
        parsePropertyType("name", true);
    }

    @Test
    public void shouldParsePathPropertyType() throws Exception {
        parsePropertyType("PATH", true);
        parsePropertyType("Path", true);
        parsePropertyType("path", true);
    }

    @Test
    public void shouldParseReferencePropertyType() throws Exception {
        parsePropertyType("REFERENCE", true);
        parsePropertyType("Reference", true);
        parsePropertyType("reference", true);
    }

    @Test
    public void shouldParseWeakReferencePropertyType() throws Exception {
        parsePropertyType("WEAKREFERENCE", true);
        parsePropertyType("WeakReference", true);
        parsePropertyType("weakreference", true);
    }

    @Test
    public void shouldParseUriPropertyType() throws Exception {
        parsePropertyType("URI", true);
        parsePropertyType("Uri", true);
        parsePropertyType("uri", true);
    }

    @Test
    public void shouldParseUndefinedPropertyType() throws Exception {
        parsePropertyType("UNDEFINED", true);
        parsePropertyType("Undefined", true);
        parsePropertyType("undefined", true);
    }

    @Test
    public void shouldNotParseInvalidPropertyType() throws Exception {
        parsePropertyType("B", false);
        parsePropertyType("b", false);
        parsePropertyType("binarytype", false);
    }

    // =============================================================================================================================
    // Attributes Parsing Tests
    // =============================================================================================================================

    private void parseAttribute( String attribute,
                                 boolean success ) throws Exception {
        CndParser parser = createParser(attribute);
        RuleReturnScope scope = parser.attributes();

        if (success) {
            assertThat("Value '" + attribute + "' did not parse and should have", scope.getTree(), notNullValue());
            assertThat(attribute, is(((org.antlr.runtime.tree.CommonTree)scope.getTree()).getText()));
        } else {
            assertThat("Value '" + attribute + "' should not parse and did", scope.getTree(), nullValue());
        }
    }

    @Test
    public void shouldParsePrimaryAttribute() throws Exception {
        parseAttribute("primary", true);
        parseAttribute("pri", true);
        parseAttribute("!", true);
    }

    @Test
    public void shouldParseAutoCreatedAttribute() throws Exception {
        parseAttribute("autocreated", true);
        parseAttribute("aut", true);
        parseAttribute("a", true);
    }

    @Test
    public void shouldParseMandatoryAttribute() throws Exception {
        parseAttribute("mandatory", true);
        parseAttribute("man", true);
        parseAttribute("m", true);
    }

    @Test
    public void shouldParseProtectedAttribute() throws Exception {
        parseAttribute("protected", true);
        parseAttribute("pro", true);
        parseAttribute("p", true);
    }

    @Test
    public void shouldParseMultipleAttribute() throws Exception {
        parseAttribute("multiple", true);
        parseAttribute("mul", true);
        parseAttribute("*", true);
    }

    @Test
    public void shouldParseCopyAttribute() throws Exception {
        parseAttribute("COPY", true);
        parseAttribute("Copy", true);
        parseAttribute("copy", true);
    }

    @Test
    public void shouldParseVersionAttribute() throws Exception {
        parseAttribute("VERSION", true);
        parseAttribute("Version", true);
        parseAttribute("version", true);
    }

    @Test
    public void shouldParseInitializeAttribute() throws Exception {
        parseAttribute("INITIALIZE", true);
        parseAttribute("Initialize", true);
        parseAttribute("initialize", true);
    }

    @Test
    public void shouldParseComputeAttribute() throws Exception {
        parseAttribute("COMPUTE", true);
        parseAttribute("Compute", true);
        parseAttribute("compute", true);
    }

    @Test
    public void shouldParseIgnoreAttribute() throws Exception {
        parseAttribute("IGNORE", true);
        parseAttribute("Ignore", true);
        parseAttribute("ignore", true);
    }

    @Test
    public void shouldParseAbortAttribute() throws Exception {
        parseAttribute("ABORT", true);
        parseAttribute("Abort", true);
        parseAttribute("abort", true);
    }

    @Test
    public void shouldNotParseInvalidAttribute() throws Exception {
        parsePropertyType("P", false);
        parsePropertyType("A", false);
        parsePropertyType("PRIMARY", false);
    }
}
