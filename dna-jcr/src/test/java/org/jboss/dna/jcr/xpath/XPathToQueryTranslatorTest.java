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
package org.jboss.dna.jcr.xpath;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import org.hamcrest.Matcher;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.query.model.QueryCommand;
import org.jboss.dna.graph.query.parse.SqlQueryParser;
import org.jboss.dna.jcr.xpath.XPath.Component;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * 
 */
public class XPathToQueryTranslatorTest {

    private ExecutionContext context;
    private XPathParser parser;

    @Before
    public void beforeEach() {
        context = new ExecutionContext();
        context.getNamespaceRegistry().register("x", "http://example.com");
        parser = new XPathParser(context);
    }

    @After
    public void afterEach() {
        parser = null;
    }

    @Test
    public void shouldTranslateXPathExpressionsToSql() {
        printSqlFor("//element(*,my:type)");
        printSqlFor("//element(nodeName,my:type)");
    }

    @Test
    public void shouldTranslateFromXPathOfAnyNode() {
        assertThat(xpath("//element(*)"), isSql("SELECT * FROM __ALLNODES__ AS nodeSet1"));
        assertThat(xpath("/jcr:root//element(*)"), isSql("SELECT * FROM __ALLNODES__ AS nodeSet1"));
        assertThat(xpath("//*"), isSql("SELECT * FROM __ALLNODES__ AS nodeSet1"));
        assertThat(xpath("/jcr:root//*"), isSql("SELECT * FROM __ALLNODES__ AS nodeSet1"));
        assertThat(xpath("//."), isSql("SELECT * FROM __ALLNODES__ AS nodeSet1"));
        assertThat(xpath("/jcr:root//."), isSql("SELECT * FROM __ALLNODES__ AS nodeSet1"));
    }

    @Test
    public void shouldTranslateFromXPathContainingExplicitPath() {
        assertThat(xpath("/jcr:root/a"),
                   isSql("SELECT * FROM __ALLNODES__ AS nodeSet1 WHERE NAME(nodeSet1) = 'a' AND DEPTH(nodeSet1) = 1"));
        assertThat(xpath("/jcr:root/a/b"), isSql("SELECT * FROM __ALLNODES__ AS nodeSet1 WHERE PATH(nodeSet1) = '/a/b'"));
        assertThat(xpath("/jcr:root/a/b/c"), isSql("SELECT * FROM __ALLNODES__ AS nodeSet1 WHERE PATH(nodeSet1) = '/a/b/c'"));
        assertThat(xpath("/jcr:root/a/b/c/d"), isSql("SELECT * FROM __ALLNODES__ AS nodeSet1 WHERE PATH(nodeSet1) = '/a/b/c/d'"));
    }

    @Test
    public void shouldTranslateFromXPathContainingExplicitPathWithChildNumbers() {
        assertThat(xpath("/jcr:root/a[2]/b"), isSql("SELECT * FROM __ALLNODES__ AS nodeSet1 WHERE PATH(nodeSet1) = '/a[2]/b'"));
        assertThat(xpath("/jcr:root/a/b[3]"), isSql("SELECT * FROM __ALLNODES__ AS nodeSet1 WHERE PATH(nodeSet1) = '/a/b[3]'"));
        assertThat(xpath("/jcr:root/a[2]/b[3]"),
                   isSql("SELECT * FROM __ALLNODES__ AS nodeSet1 WHERE PATH(nodeSet1) = '/a[2]/b[3]'"));
    }

    @Test
    public void shouldTranslateFromXPathContainingExplicitPathWithWildcardChildNumbers() {
        assertThat(xpath("/jcr:root/a[*]/b"), isSql("SELECT * FROM __ALLNODES__ AS nodeSet1 WHERE PATH(nodeSet1) = '/a/b'"));
        assertThat(xpath("/jcr:root/a/b[*]"), isSql("SELECT * FROM __ALLNODES__ AS nodeSet1 WHERE PATH(nodeSet1) = '/a/b'"));
        assertThat(xpath("/jcr:root/a[*]/b[*]"), isSql("SELECT * FROM __ALLNODES__ AS nodeSet1 WHERE PATH(nodeSet1) = '/a/b'"));
    }

    @Test
    public void shouldTranslateFromXPathContainingPathWithDescendantOrSelf() {
        assertThat(xpath("/jcr:root/a/b//c"),
                   isSql("SELECT * FROM __ALLNODES__ AS nodeSet1 WHERE PATH(nodeSet1) = '/a/b/c' OR PATH(nodeSet1) LIKE '/a/b/%/c'"));
        assertThat(xpath("/jcr:root/a/b[2]//c"),
                   isSql("SELECT * FROM __ALLNODES__ AS nodeSet1 WHERE PATH(nodeSet1) = '/a/b[2]/c' OR PATH(nodeSet1) LIKE '/a/b[2]/%/c'"));
        assertThat(xpath("/jcr:root/a/b//c[4]"),
                   isSql("SELECT * FROM __ALLNODES__ AS nodeSet1 WHERE PATH(nodeSet1) = '/a/b/c[4]' OR PATH(nodeSet1) LIKE '/a/b/%/c[4]'"));
        assertThat(xpath("/jcr:root/a/b[2]//c[4]"),
                   isSql("SELECT * FROM __ALLNODES__ AS nodeSet1 WHERE PATH(nodeSet1) = '/a/b[2]/c[4]' OR PATH(nodeSet1) LIKE '/a/b[2]/%/c[4]'"));
    }

    @Test
    public void shouldTranslateFromXPathContainingPathWithMultipleDescendantOrSelf() {
        assertThat(xpath("/jcr:root/a/b//c//d"),
                   isSql("SELECT * FROM __ALLNODES__ AS nodeSet1 WHERE (((PATH(nodeSet1) = '/a/b/c/d' OR PATH(nodeSet1) LIKE '/a/b/%/c/d') OR PATH(nodeSet1) LIKE '/a/b/c/%/d') OR PATH(nodeSet1) LIKE '/a/b/%/c/%/d')"));
    }

    @Test
    public void shouldTranslateFromXPathContainingPredicatesUsingRelativePaths() {
        assertThat(xpath("//element(*,my:type)[a/@id]"),
                   isSql("SELECT * FROM [my:type] JOIN __ALLNODES__ as nodeSet1 ON ISCHILDNODE(nodeSet1,[my:type]) WHERE NAME(nodeSet1) = 'a' AND nodeSet1.id IS NOT NULL"));
        assertThat(xpath("//element(*,my:type)[a/b/@id]"),
                   isSql("SELECT * FROM [my:type] JOIN __ALLNODES__ as nodeSet1 ON ISCHILDNODE(nodeSet1,[my:type]) JOIN __ALLNODES__ as nodeSet2 ON ISCHILDNODE(nodeSet2,nodeSet1) WHERE (NAME(nodeSet1) = 'a' AND NAME(nodeSet2) = 'b') AND nodeSet2.id IS NOT NULL"));
        assertThat(xpath("//element(*,my:type)[a/b/((@id and @name) or not(@address))]"),
                   isSql("SELECT * FROM [my:type] JOIN __ALLNODES__ as nodeSet1 ON ISCHILDNODE(nodeSet1,[my:type]) JOIN __ALLNODES__ as nodeSet2 ON ISCHILDNODE(nodeSet2,nodeSet1) WHERE (NAME(nodeSet1) = 'a' AND NAME(nodeSet2) = 'b') AND ((nodeSet2.id IS NOT NULL and nodeSet2.name IS NOT NULL) OR (NOT(nodeSet2.address IS NOT NULL)))"));
        assertThat(xpath("//element(*,my:type)[./a/b/((@id and @name) or not(@address))]"),
                   isSql("SELECT * FROM [my:type] JOIN __ALLNODES__ as nodeSet1 ON ISCHILDNODE(nodeSet1,[my:type]) JOIN __ALLNODES__ as nodeSet2 ON ISCHILDNODE(nodeSet2,nodeSet1) WHERE (NAME(nodeSet1) = 'a' AND NAME(nodeSet2) = 'b') AND ((nodeSet2.id IS NOT NULL and nodeSet2.name IS NOT NULL) OR (NOT(nodeSet2.address IS NOT NULL)))"));
        assertThat(xpath("//element(*,my:type)[a/b/((@id and @name) or not(jcr:contains(@desc,'rock star')))]"),
                   isSql("SELECT * FROM [my:type] JOIN __ALLNODES__ as nodeSet1 ON ISCHILDNODE(nodeSet1,[my:type]) JOIN __ALLNODES__ as nodeSet2 ON ISCHILDNODE(nodeSet2,nodeSet1) WHERE (NAME(nodeSet1) = 'a' AND NAME(nodeSet2) = 'b') AND ((nodeSet2.id IS NOT NULL and nodeSet2.name IS NOT NULL) OR (NOT(CONTAINS(nodeSet2.desc,'rock star'))))"));
        assertThat(xpath("//element(*,my:type)[*/@id]"),
                   isSql("SELECT * FROM [my:type] JOIN __ALLNODES__ as nodeSet1 ON ISCHILDNODE(nodeSet1,[my:type]) WHERE nodeSet1.id IS NOT NULL"));
        assertThat(xpath("//element(*,my:type)[*/*/@id]"),
                   isSql("SELECT * FROM [my:type] JOIN __ALLNODES__ as nodeSet1 ON ISCHILDNODE(nodeSet1,[my:type]) JOIN __ALLNODES__ as nodeSet2 ON ISCHILDNODE(nodeSet2,nodeSet1) WHERE nodeSet2.id IS NOT NULL"));
        assertThat(xpath("//element(*,my:type)[./*/*/@id]"),
                   isSql("SELECT * FROM [my:type] JOIN __ALLNODES__ as nodeSet1 ON ISCHILDNODE(nodeSet1,[my:type]) JOIN __ALLNODES__ as nodeSet2 ON ISCHILDNODE(nodeSet2,nodeSet1) WHERE nodeSet2.id IS NOT NULL"));
        assertThat(xpath("//element(*,my:type)[.//@id]"),
                   isSql("SELECT * FROM [my:type] JOIN __ALLNODES__ as nodeSet1 ON ISDESCENDANTNODE(nodeSet1,[my:type]) WHERE nodeSet1.id IS NOT NULL"));
    }

    @Test
    public void shouldTranslateFromXPathContainingPredicatesIdentifyingPropertiesThatMustHaveValues() {
        assertThat(xpath("//element(*,my:type)[@id]"), isSql("SELECT * FROM [my:type] WHERE id IS NOT NULL"));
        assertThat(xpath("//element(*,my:type)[@id][@name]"),
                   isSql("SELECT * FROM [my:type] WHERE id IS NOT NULL AND name IS NOT NULL"));
        assertThat(xpath("//element(*,my:type)[@id | @name]"),
                   isSql("SELECT * FROM [my:type] WHERE id IS NOT NULL OR name IS NOT NULL"));
        assertThat(xpath("//element(*,my:type)[@id | (@name and @address)]"),
                   isSql("SELECT * FROM [my:type] WHERE id IS NOT NULL OR (name IS NOT NULL AND address IS NOT NULL)"));
    }

    @Test
    public void shouldTranslateFromXPathContainingPredicatesUsingNot() {
        assertThat(xpath("//element(*,my:type)[not(@id)]"), isSql("SELECT * FROM [my:type] WHERE NOT(id IS NOT NULL)"));
        assertThat(xpath("//element(*,my:type)[not(jcr:contains(@desc,'rock star'))]"),
                   isSql("SELECT * FROM [my:type] WHERE NOT(CONTAINS(desc,'rock star'))"));
        assertThat(xpath("//element(*,my:type)[not(@id < 1 and jcr:contains(@desc,'rock star'))]"),
                   isSql("SELECT * FROM [my:type] WHERE NOT(id < 1 AND CONTAINS(desc,'rock star'))"));
    }

    @Test
    public void shouldTranslateFromXPathContainingPredicatesIdentifyingPropertyCriteria() {
        assertThat(xpath("//element(*,my:type)[@id = 1]"), isSql("SELECT * FROM [my:type] WHERE id = 1"));
        assertThat(xpath("//element(*,my:type)[@id < 1 and @name = 'john']"),
                   isSql("SELECT * FROM [my:type] WHERE id < 1 AND name = 'john'"));
        assertThat(xpath("//element(*,my:type)[@id < 1 and ( @name = 'john' or @name = 'mary')]"),
                   isSql("SELECT * FROM [my:type] WHERE id < 1 AND (name = 'john' OR name = 'mary')"));
        assertThat(xpath("//element(*,my:type)[@id < 1 and ( jcr:like(@name,'%john') or @name = 'mary')]"),
                   isSql("SELECT * FROM [my:type] WHERE id < 1 AND (name like '%john' OR name = 'mary')"));
        assertThat(xpath("//element(*,my:type)[@id < 1 and jcr:contains(@desc,'rock star')]"),
                   isSql("SELECT * FROM [my:type] WHERE id < 1 AND CONTAINS(desc,'rock star')"));
    }

    @Test
    public void shouldTranslateFromXPathContainingPredicatesIdentifyingPropertyCriteriaWithTypeCasts() {
        assertThat(xpath("//element(*,my:type)[@datestart<=xs:dateTime('2009-09-24T11:53:23.293-05:00')]"),
                   isSql("SELECT * FROM [my:type] WHERE datestart <= CAST('2009-09-24T11:53:23.293-05:00' AS DATE)"));
        assertThat(xpath("//element(*,my:type)[@prop<=xs:boolean('true')]"),
                   isSql("SELECT * FROM [my:type] WHERE prop <= CAST('true' AS BOOLEAN)"));
    }

    @Test
    public void shouldTranslateFromXPathContainingAttributesInPathIdentifyingPropertiesToBeSelected() {
        assertThat(xpath("//element(*,my:type)/@id"), isSql("SELECT id FROM [my:type]"));
        assertThat(xpath("//element(*,my:type)/(@id|@name)"), isSql("SELECT id, name FROM [my:type]"));
        assertThat(xpath("//element(*,my:type)/(@id|@x:address)"), isSql("SELECT id, [x:address] FROM [my:type]"));
        assertThat(xpath("//element(*,my:type)/(@id|@name|@x:address)"), isSql("SELECT id, name, [x:address] FROM [my:type]"));
        assertThat(xpath("//element(*,my:type)/(@id union @name)"), isSql("SELECT id, name FROM [my:type]"));
        assertThat(xpath("//element(*,my:type)/(@id union @name union @x:address)"),
                   isSql("SELECT id, name, [x:address] FROM [my:type]"));
        assertThat(xpath("//(@id|@name)"), isSql("SELECT nodeSet1.id, nodeSet1.name FROM __ALLNODES__ AS nodeSet1"));
        assertThat(xpath("//./(@id|@name)"), isSql("SELECT nodeSet1.id, nodeSet1.name FROM __ALLNODES__ AS nodeSet1"));
    }

    @Test
    public void shouldTranslateFromXPathOfAnyNodeOfSpecificType() {
        assertThat(xpath("//element(*,my:type)"), isSql("SELECT * FROM [my:type]"));
    }

    @Test
    public void shouldTranslateFromXPathOfAnyNodeOfSpecificTypeAndWithSpecificName() {
        assertThat(xpath("//element(nodeName,my:type)"), isSql("SELECT * FROM [my:type] WHERE NAME([my:type]) = 'nodeName'"));
    }

    @Test
    public void shouldTranslateFromXPathOfAnyNodeWithName() {
        assertThat(xpath("//element(nodeName,*)"),
                   isSql("SELECT * FROM __ALLNODES__ AS nodeSet1 WHERE NAME(nodeSet1) = 'nodeName'"));

        assertThat(xpath("//element(nodeName,*)"),
                   isSql("SELECT * FROM __ALLNODES__ AS nodeSet1 WHERE NAME(nodeSet1) = 'nodeName'"));

        assertThat(xpath("//nodeName"), isSql("SELECT * FROM __ALLNODES__ AS nodeSet1 WHERE NAME(nodeSet1) = 'nodeName'"));

        assertThat(xpath("/jcr:root//element(nodeName,*)"),
                   isSql("SELECT * FROM __ALLNODES__ AS nodeSet1 WHERE NAME(nodeSet1) = 'nodeName'"));

        assertThat(xpath("/jcr:root//nodeName"),
                   isSql("SELECT * FROM __ALLNODES__ AS nodeSet1 WHERE NAME(nodeSet1) = 'nodeName'"));
    }

    @Test
    public void shouldTranslateFromXPathOfNodeWithNameUnderRoot() {
        assertThat(xpath("/jcr:root/element(nodeName,*)"),
                   isSql("SELECT * FROM __ALLNODES__ AS nodeSet1 WHERE NAME(nodeSet1) = 'nodeName' AND DEPTH(nodeSet1) = 1"));

        assertThat(xpath("/jcr:root/nodeName"),
                   isSql("SELECT * FROM __ALLNODES__ AS nodeSet1 WHERE NAME(nodeSet1) = 'nodeName' AND DEPTH(nodeSet1) = 1"));

        assertThat(xpath("nodeName"),
                   isSql("SELECT * FROM __ALLNODES__ AS nodeSet1 WHERE NAME(nodeSet1) = 'nodeName' AND DEPTH(nodeSet1) = 1"));
    }

    @Test
    public void shouldTranslateFromXPathOfAnyNodeUsingPredicate() {
        assertThat(xpath("//.[jcr:contains(.,'bar')]"),
                   isSql("SELECT * FROM __ALLNODES__ AS nodeSet1 WHERE CONTAINS(nodeSet1.*,'bar')"));
        assertThat(xpath("//.[jcr:contains(a,'bar')]"),
                   isSql("SELECT * FROM __ALLNODES__ AS nodeSet1 JOIN __ALLNODES__ AS nodeSet2 ON ISCHILDNODE(nodeSet2,nodeSet1) WHERE CONTAINS(nodeSet2.*,'bar')"));
        assertThat(xpath("//*[jcr:contains(.,'bar')]"),
                   isSql("SELECT * FROM __ALLNODES__ AS nodeSet1 WHERE CONTAINS(nodeSet1.*,'bar')"));
        assertThat(xpath("//*[jcr:contains(a,'bar')]"),
                   isSql("SELECT * FROM __ALLNODES__ AS nodeSet1 JOIN __ALLNODES__ AS nodeSet2 ON ISCHILDNODE(nodeSet2,nodeSet1) WHERE CONTAINS(nodeSet2.*,'bar')"));
        assertThat(xpath("//*[jcr:contains(a/@b,'bar')]"),
                   isSql("SELECT * FROM __ALLNODES__ AS nodeSet1 JOIN __ALLNODES__ AS nodeSet2 ON ISCHILDNODE(nodeSet2,nodeSet1) WHERE NAME(nodeSet2) = 'a' AND CONTAINS(nodeSet2.b,'bar')"));
        assertThat(xpath("//*[jcr:contains(a/*/@b,'bar')]"),
                   isSql("SELECT * FROM __ALLNODES__ AS nodeSet1 JOIN __ALLNODES__ AS nodeSet2 ON ISCHILDNODE(nodeSet2,nodeSet1) JOIN __ALLNODES__ AS nodeSet3 ON ISCHILDNODE(nodeSet3,nodeSet2) WHERE NAME(nodeSet2) = 'a' AND CONTAINS(nodeSet3.b,'bar')"));
        assertThat(xpath("/jcr:root//element(*)[jcr:contains(a/@b,'bar')]"),
                   isSql("SELECT * FROM __ALLNODES__ AS nodeSet1 JOIN __ALLNODES__ AS nodeSet2 ON ISCHILDNODE(nodeSet2,nodeSet1) WHERE NAME(nodeSet2) = 'a' AND CONTAINS(nodeSet2.b,'bar')"));
        assertThat(xpath("/jcr:root//element(*)[jcr:contains(a/*/@b,'bar')]"),
                   isSql("SELECT * FROM __ALLNODES__ AS nodeSet1 JOIN __ALLNODES__ AS nodeSet2 ON ISCHILDNODE(nodeSet2,nodeSet1) JOIN __ALLNODES__ AS nodeSet3 ON ISCHILDNODE(nodeSet3,nodeSet2) WHERE NAME(nodeSet2) = 'a' AND CONTAINS(nodeSet3.b,'bar')"));
        assertThat(xpath("/jcr:root//*[jcr:contains(a/@b,'bar')]"),
                   isSql("SELECT * FROM __ALLNODES__ AS nodeSet1 JOIN __ALLNODES__ AS nodeSet2 ON ISCHILDNODE(nodeSet2,nodeSet1) WHERE NAME(nodeSet2) = 'a' AND CONTAINS(nodeSet2.b,'bar')"));
        assertThat(xpath("/jcr:root//*[jcr:contains(a/*/@b,'bar')]"),
                   isSql("SELECT * FROM __ALLNODES__ AS nodeSet1 JOIN __ALLNODES__ AS nodeSet2 ON ISCHILDNODE(nodeSet2,nodeSet1) JOIN __ALLNODES__ AS nodeSet3 ON ISCHILDNODE(nodeSet3,nodeSet2) WHERE NAME(nodeSet2) = 'a' AND CONTAINS(nodeSet3.b,'bar')"));
    }

    // @Test
    // public void shouldParseXPathExpressions() {
    // assertParsable("/jcr:root/a/b/c");
    // assertParsable("/jcr:root/a/b/c[*]");
    // assertParsable("/jcr:root/some[1]/element(nodes, my:type)[1]");
    // assertParsable("//element(*,my:type)");
    // assertParsable("//element(*,my:type)[@jcr:title='something' and @globalProperty='something else']");
    // assertParsable("//element(*,my:type)[@jcr:title | @globalProperty]");
    // assertParsable("//element(*, my:type) order by @my:title");
    // assertParsable("//element(*, my:type) [jcr:contains(., 'jcr')] order by jcr:score() descending");
    // assertParsable("//element(*, employee)[@secretary and @assistant]");
    // }

    // ----------------------------------------------------------------------------------------------------------------
    // utility methods
    // ----------------------------------------------------------------------------------------------------------------

    protected void printSqlFor( String xpath ) {
        System.out.println("XPath: " + xpath);
        System.out.println("SQL:   " + translateToSql(xpath));
        System.out.println();
    }

    private QueryCommand translateToSql( String xpath ) {
        Component component = parser.parseXPath(xpath);
        XPathToQueryTranslator translator = new XPathToQueryTranslator(context, xpath);
        return translator.createQuery(component);
    }

    private QueryCommand xpath( String xpath ) {
        Component component = parser.parseXPath(xpath);
        XPathToQueryTranslator translator = new XPathToQueryTranslator(context, xpath);
        return translator.createQuery(component);
    }

    protected QueryCommand sql( String sql ) {
        return new SqlQueryParser().parseQuery(sql, context);
    }

    protected Matcher<QueryCommand> isSql( String sql ) {
        return is(sql(sql));
    }

}
