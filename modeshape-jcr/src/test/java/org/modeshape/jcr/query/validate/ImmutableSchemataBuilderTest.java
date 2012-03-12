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
package org.modeshape.jcr.query.validate;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.jcr.ExecutionContext;
import org.modeshape.jcr.query.model.SelectorName;
import org.modeshape.jcr.query.validate.Schemata.Table;
import org.modeshape.jcr.query.validate.Schemata.View;
import org.modeshape.jcr.value.PropertyType;

/**
 * 
 */
public class ImmutableSchemataBuilderTest {

    private static final String STRING_TYPE = PropertyType.STRING.getName().toUpperCase();

    private ExecutionContext context;
    private ImmutableSchemata.Builder builder;
    private Schemata schemata;

    @Before
    public void beforeEach() {
        context = new ExecutionContext();
        builder = ImmutableSchemata.createBuilder(context);
    }

    @After
    public void afterEach() {
        context = null;
        builder = null;
        schemata = null;
    }

    @Test
    public void shouldBuildSchemaForSingleTableWithDefaultTypeForColumns() {
        builder.addTable("t1", "c1", "c2", "c3");
        schemata = builder.build();
        Table table = schemata.getTable(selector("t1"));
        assertThat(table, is(notNullValue()));
        assertThat(table.getColumn("c1").getPropertyTypeName(), is(STRING_TYPE));
        assertThat(table.getColumn("c2").getPropertyTypeName(), is(STRING_TYPE));
        assertThat(table.getColumn("c3").getPropertyTypeName(), is(STRING_TYPE));
    }

    @Test
    public void shouldBuildSchemaForMultipleTablesWithDefaultTypeForColumns() {
        builder.addTable("t1", "c11", "c12", "c13");
        builder.addTable("t2", "c21", "c22", "c23");
        schemata = builder.build();
        Table table = schemata.getTable(selector("t1"));
        assertThat(table, is(notNullValue()));
        assertThat(table.getColumn("c11").getPropertyTypeName(), is(STRING_TYPE));
        assertThat(table.getColumn("c12").getPropertyTypeName(), is(STRING_TYPE));
        assertThat(table.getColumn("c13").getPropertyTypeName(), is(STRING_TYPE));
        table = schemata.getTable(selector("t2"));
        assertThat(table, is(notNullValue()));
        assertThat(table.getColumn("c21").getPropertyTypeName(), is(STRING_TYPE));
        assertThat(table.getColumn("c22").getPropertyTypeName(), is(STRING_TYPE));
        assertThat(table.getColumn("c23").getPropertyTypeName(), is(STRING_TYPE));
    }

    @Test
    public void shouldBuildSchemaContainingViewOfSingleTable() {
        builder.addTable("t1", "c1", "c2", "c3");
        builder.addView("t2", "SELECT * FROM t1 WHERE c1=3");
        schemata = builder.build();
        Table table = schemata.getTable(selector("t1"));
        assertThat(table, is(notNullValue()));
        assertThat(table.getColumn("c1").getPropertyTypeName(), is(STRING_TYPE));
        assertThat(table.getColumn("c2").getPropertyTypeName(), is(STRING_TYPE));
        assertThat(table.getColumn("c3").getPropertyTypeName(), is(STRING_TYPE));
        table = schemata.getTable(selector("t2"));
        assertThat(table, is(instanceOf(View.class)));
        assertThat(table.getColumn("c1").getPropertyTypeName(), is(STRING_TYPE));
        assertThat(table.getColumn("c2").getPropertyTypeName(), is(STRING_TYPE));
        assertThat(table.getColumn("c3").getPropertyTypeName(), is(STRING_TYPE));
    }

    @Test
    public void shouldBuildSchemaContainingViewUsingAliasesOfColumns() {
        builder.addTable("t1", "c1", "c2", "c3");
        builder.addView("t2", "SELECT c1 as v1, c2 FROM t1 WHERE c1=3");
        schemata = builder.build();
        Table table = schemata.getTable(selector("t1"));
        assertThat(table, is(notNullValue()));
        assertThat(table.getColumn("c1").getPropertyTypeName(), is(STRING_TYPE));
        assertThat(table.getColumn("c2").getPropertyTypeName(), is(STRING_TYPE));
        assertThat(table.getColumn("c3").getPropertyTypeName(), is(STRING_TYPE));
        table = schemata.getTable(selector("t2"));
        assertThat(table, is(instanceOf(View.class)));
        assertThat(table.getColumn("v1").getPropertyTypeName(), is(STRING_TYPE));
        assertThat(table.getColumn("c2").getPropertyTypeName(), is(STRING_TYPE));
    }

    @Test
    public void shouldBuildSchemaContainingViewOfMultiTableJoin() {
        builder.addTable("t1", "c11", "c12", "c13");
        builder.addTable("t2", "c21", "c22", "c23");
        builder.addView("v1", "SELECT t1.c11 as x1, t1.c12, t2.c23 FROM t1 JOIN t2 ON t1.c11=t2.c21 WHERE t1.c11=3");
        schemata = builder.build();
        Table table = schemata.getTable(selector("t1"));
        assertThat(table, is(notNullValue()));
        assertThat(table.getColumn("c11").getPropertyTypeName(), is(STRING_TYPE));
        assertThat(table.getColumn("c12").getPropertyTypeName(), is(STRING_TYPE));
        assertThat(table.getColumn("c13").getPropertyTypeName(), is(STRING_TYPE));
        table = schemata.getTable(selector("t2"));
        assertThat(table, is(notNullValue()));
        assertThat(table.getColumn("c21").getPropertyTypeName(), is(STRING_TYPE));
        assertThat(table.getColumn("c22").getPropertyTypeName(), is(STRING_TYPE));
        assertThat(table.getColumn("c23").getPropertyTypeName(), is(STRING_TYPE));
        schemata = builder.build();
        table = schemata.getTable(selector("v1"));
        assertThat(table, is(instanceOf(View.class)));
        assertThat(table.getColumn("x1").getPropertyTypeName(), is(STRING_TYPE));
        assertThat(table.getColumn("c12").getPropertyTypeName(), is(STRING_TYPE));
        assertThat(table.getColumn("c23").getPropertyTypeName(), is(STRING_TYPE));
    }

    @Test
    public void shouldBuildSchemaContainingViewUsingView() {
        builder.addTable("t1", "c11", "c12", "c13");
        builder.addTable("t2", "c21", "c22", "c23");
        builder.addView("v2", "SELECT x1, c12 FROM v1 WHERE x1=3");
        builder.addView("v1", "SELECT t1.c11 as x1, t1.c12, t2.c23 FROM t1 JOIN t2 ON t1.c11=t2.c21 WHERE t1.c11=3");
        schemata = builder.build();
        Table table = schemata.getTable(selector("t1"));
        assertThat(table, is(notNullValue()));
        assertThat(table.getColumn("c11").getPropertyTypeName(), is(STRING_TYPE));
        assertThat(table.getColumn("c12").getPropertyTypeName(), is(STRING_TYPE));
        assertThat(table.getColumn("c13").getPropertyTypeName(), is(STRING_TYPE));
        table = schemata.getTable(selector("t2"));
        assertThat(table, is(notNullValue()));
        assertThat(table.getColumn("c21").getPropertyTypeName(), is(STRING_TYPE));
        assertThat(table.getColumn("c22").getPropertyTypeName(), is(STRING_TYPE));
        assertThat(table.getColumn("c23").getPropertyTypeName(), is(STRING_TYPE));
        schemata = builder.build();
        table = schemata.getTable(selector("v1"));
        assertThat(table, is(instanceOf(View.class)));
        assertThat(table.getColumn("x1").getPropertyTypeName(), is(STRING_TYPE));
        assertThat(table.getColumn("c12").getPropertyTypeName(), is(STRING_TYPE));
        assertThat(table.getColumn("c23").getPropertyTypeName(), is(STRING_TYPE));
        table = schemata.getTable(selector("v2"));
        assertThat(table, is(instanceOf(View.class)));
        assertThat(table.getColumn("x1").getPropertyTypeName(), is(STRING_TYPE));
        assertThat(table.getColumn("c12").getPropertyTypeName(), is(STRING_TYPE));
    }

    protected SelectorName selector( String name ) {
        return new SelectorName(name);
    }

}
