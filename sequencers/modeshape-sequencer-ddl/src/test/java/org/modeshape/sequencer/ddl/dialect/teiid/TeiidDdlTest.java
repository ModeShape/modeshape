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
package org.modeshape.sequencer.ddl.dialect.teiid;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import org.modeshape.sequencer.ddl.DdlTokenStream;
import org.modeshape.sequencer.ddl.node.AstNode;

public abstract class TeiidDdlTest {

    protected void assertMixinType( final AstNode node,
                                    final String expectedNodeType ) {
        assertThat(hasMixin(node, expectedNodeType), is(true));
    }

    protected void assertProperty( final AstNode node,
                                   final String name,
                                   final Object expectedValue ) {
        Object actualValue = node.getProperty(name);
        assertThat(actualValue, is(expectedValue));
    }

    protected DdlTokenStream getTokens( final String content ) {
        final DdlTokenStream tokens = new DdlTokenStream(content, DdlTokenStream.ddlTokenizer(false), false);
        tokens.start();
        return tokens;
    }

    protected boolean hasMixin( final AstNode node,
                                final String mixin ) {
        return node.getMixins().contains(mixin);
    }

}
