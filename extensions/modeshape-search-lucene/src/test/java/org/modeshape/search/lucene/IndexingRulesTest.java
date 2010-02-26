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
package org.modeshape.search.lucene;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import org.apache.lucene.document.Field;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.search.lucene.IndexRules.Builder;

/**
 * 
 */
public class IndexingRulesTest {

    private Builder builder;
    private IndexRules rules;

    @Before
    public void beforeEach() {
        builder = IndexRules.createBuilder();
        rules = builder.build();
    }

    @Test
    public void shouldBuildValidRulesFromBuilderThatIsNotInvoked() {
        builder = IndexRules.createBuilder();
        rules = builder.build();
    }

    @Test
    public void shouldBuildValidRulesFromBuilderAfterJustSettingDefaultRules() {
        builder.defaultTo(Field.Store.NO, Field.Index.ANALYZED_NO_NORMS, false, false);
        rules = builder.build();
        assertThat(rules.getRule(null).getIndexOption(), is(Field.Index.ANALYZED_NO_NORMS));
        assertThat(rules.getRule(null).getStoreOption(), is(Field.Store.NO));
    }
}
