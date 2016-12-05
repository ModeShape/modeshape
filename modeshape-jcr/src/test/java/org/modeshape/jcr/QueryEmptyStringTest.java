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
package org.modeshape.jcr;

import java.util.ArrayList;
import java.util.List;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import org.junit.Assert;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * See MODE-2174: JCR Query in Modeshape returns no result when empty string used in comparison
 */
public class QueryEmptyStringTest extends MultiUseAbstractTest {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private static final String SQL = "SELECT [jcr:name], [car:model], [car:maker] FROM [car:Car] ";


    @BeforeClass
    public static void setUp() throws Exception {
        RepositoryConfiguration config = new RepositoryConfiguration("config/simple-repo-config.json");
        startRepository(config);
        registerNodeTypes("cnd/cars.cnd");

        Node root = session.getRootNode();
        Node item;

        item = root.addNode("Aston Martin", "car:Car");
        item.setProperty("car:maker", "Aston Martin");
        item.setProperty("car:model", "DB9");

        item = root.addNode("Infiniti", "car:Car");
        item.setProperty("car:maker", "Infiniti");

        item = root.addNode("EMPTY", "car:Car");
        item.setProperty("car:maker", "");

        item = root.addNode("NULL", "car:Car");

        /**
         * jcr:name          |    car:maker         |    car:model
         * -----------------------------------------------------------
         * 'Aston Martin'    |    'Aston Martin'    |    'DB9'
         * 'Infiniti'        |    'Infiniti'        |    null
         * 'EMPTY'           |    ''                |    null
         * 'NULL'            |    null              |    null
         */

        session.save();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        stopRepository();
    }

    private List<String> collectNames(QueryResult result) throws RepositoryException {
        NodeIterator iterator = result.getNodes();
        List<String> actual = new ArrayList<String>();
        while (iterator.hasNext()) actual.add(iterator.nextNode().getName());

        return actual;
    }


    @Test
    public void shouldEqualsEmpty() throws Exception {
        String sql = SQL + "WHERE [car:maker] = ''";
        logger.debug("\nSQL: {}", sql);

        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        QueryResult result = query.execute();

        printResults(result);

        Assert.assertEquals("Should contains rows", 1, result.getRows().getSize());
        List<String> actual = collectNames(result);
        Assert.assertTrue("Should contains EMPTY", actual.contains("EMPTY"));
    }

    @Test
    public void shouldIsNotNull() throws Exception {
        String sql = SQL + "WHERE [car:maker] IS NOT NULL";
        logger.debug("\nSQL: {}", sql);

        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        QueryResult result = query.execute();

        printResults(result);

        Assert.assertEquals("Should contains rows", 3, result.getRows().getSize());
        List<String> actual = collectNames(result);
        Assert.assertTrue("Should contains EMPTY", actual.contains("EMPTY"));
        Assert.assertTrue("Should contains Aston Martin", actual.contains("Aston Martin"));
        Assert.assertTrue("Should contains Infiniti", actual.contains("Infiniti"));
    }

    @Test
    public void shouldIsNull() throws Exception {
        String sql = SQL + "WHERE [car:maker] IS NULL";
        logger.debug("\nSQL: {}", sql);

        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        QueryResult result = query.execute();

        printResults(result);

        Assert.assertEquals(1, result.getRows().getSize());
        List<String> actual = collectNames(result);
        Assert.assertTrue("Should contains NULL", actual.contains("NULL"));
    }

    @Test
    public void shouldLengthEqualsZero() throws Exception {
        String sql = SQL + "WHERE LENGTH([car:maker]) = 0";
        logger.debug("\nSQL: {}", sql);

        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        QueryResult result = query.execute();

        printResults(result);

        Assert.assertEquals(1, result.getRows().getSize());
        List<String> actual = collectNames(result);
        Assert.assertTrue("Should contains EMPTY", actual.contains("EMPTY"));
    }

    @Test
    public void shouldLengthGreaterZero() throws Exception {
        String sql = SQL + "WHERE LENGTH([car:maker]) > 0";
        logger.debug("\nSQL: {}", sql);

        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        QueryResult result = query.execute();

        printResults(result);

        Assert.assertEquals(2, result.getRows().getSize());
        List<String> actual = collectNames(result);
        Assert.assertTrue("Should contains Aston Martin", actual.contains("Aston Martin"));
        Assert.assertTrue("Should contains Infiniti", actual.contains("Infiniti"));
    }

    @Test
    public void shouldLengthGreaterOrEqualsZero() throws Exception {
        String sql = SQL + "WHERE LENGTH([car:maker]) >= 0";
        logger.debug("\nSQL: {}", sql);

        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        QueryResult result = query.execute();

        printResults(result);

        Assert.assertEquals(3, result.getRows().getSize());
        List<String> actual = collectNames(result);
        Assert.assertTrue("Should contains EMPTY", actual.contains("EMPTY"));
        Assert.assertTrue("Should contains Aston Martin", actual.contains("Aston Martin"));
        Assert.assertTrue("Should contains Infiniti", actual.contains("Infiniti"));
    }

    @Test
    public void shouldLikeEmpty() throws Exception {
        String sql = SQL + "WHERE [car:maker] LIKE ''";
        logger.debug("\nSQL: {}", sql);

        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        QueryResult result = query.execute();

        printResults(result);

        Assert.assertEquals(1, result.getRows().getSize());
        List<String> actual = collectNames(result);
        Assert.assertTrue("Should contains EMPTY", actual.contains("EMPTY"));
    }

    @Test
    public void shouldInEmpty() throws Exception {
        String sql = SQL + "WHERE [car:maker] IN ('')";
        logger.debug("\nSQL: {}", sql);

        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        QueryResult result = query.execute();

        printResults(result);

        Assert.assertEquals(1, result.getRows().getSize());
        List<String> actual = collectNames(result);
        Assert.assertTrue("Should contains EMPTY", actual.contains("EMPTY"));
    }

    @Test
    public void shouldInEmptyAndString() throws Exception {
        String sql = SQL + "WHERE [car:maker] IN ('Aston Martin', '')";
        logger.debug("\nSQL: {}", sql);

        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        QueryResult result = query.execute();

        printResults(result);


        Assert.assertEquals("Should contains rows", 2, result.getRows().getSize());
        List<String> actual = collectNames(result);
        Assert.assertTrue("Should contains EMPTY", actual.contains("EMPTY"));
        Assert.assertTrue("Should contains Aston Martin", actual.contains("Aston Martin"));
    }

    @Test
    public void shouldOrEqualsEmpty() throws Exception {
        String sql = SQL + "WHERE [car:maker] = '' OR [car:maker] = 'Infiniti'";
        logger.debug("\nSQL: {}", sql);

        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        QueryResult result = query.execute();

        printResults(result);


        Assert.assertEquals("Should contains rows", 2, result.getRows().getSize());
        List<String> actual = collectNames(result);
        Assert.assertTrue("Should contains EMPTY", actual.contains("EMPTY"));
        Assert.assertTrue("Should contains Infiniti", actual.contains("Infiniti"));
    }

    @Test
    public void shouldOrIsNull() throws Exception {
        String sql = SQL + "WHERE [car:maker] IS NULL OR [car:model] = 'DB9'";
        logger.debug("\nSQL: {}", sql);

        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        QueryResult result = query.execute();

        printResults(result);


        Assert.assertEquals("Should contains rows", 2, result.getRows().getSize());
        List<String> actual = collectNames(result);
        Assert.assertTrue("Should contains Aston Martin", actual.contains("Aston Martin"));
        Assert.assertTrue("Should contains NULL", actual.contains("NULL"));
    }

    @Test
    public void shouldOrIsNotNull() throws Exception {
        String sql = SQL + "WHERE [car:maker] IS NOT NULL OR [car:model] = 'DB9'";
        logger.debug("\nSQL: {}", sql);

        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        QueryResult result = query.execute();

        printResults(result);


        Assert.assertEquals("Should contains rows", 3, result.getRows().getSize());
        List<String> actual = collectNames(result);
        Assert.assertTrue("Should contains EMPTY", actual.contains("EMPTY"));
        Assert.assertTrue("Should contains Infiniti", actual.contains("Infiniti"));
        Assert.assertTrue("Should contains Aston Martin", actual.contains("Aston Martin"));
    }

    @Test
    public void shouldAndEqualsEmpty() throws Exception {
        String sql = SQL + "WHERE [car:maker] = '' AND [car:model] IS NULL";
        logger.debug("\nSQL: {}", sql);

        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        QueryResult result = query.execute();

        printResults(result);

        Assert.assertEquals("Should contains rows", 1, result.getRows().getSize());
        List<String> actual = collectNames(result);
        Assert.assertTrue("Should contains EMPTY", actual.contains("EMPTY"));
    }

    @Test
    public void shouldAndIsNotNull() throws Exception {
        String sql = SQL + "WHERE [car:maker] IS NOT NULL AND [car:model] = 'DB9'";
        logger.debug("\nSQL: {}", sql);

        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        QueryResult result = query.execute();

        printResults(result);

        Assert.assertEquals("Should contains rows", 1, result.getRows().getSize());
        List<String> actual = collectNames(result);
        Assert.assertTrue("Should contains Aston Martin", actual.contains("Aston Martin"));
    }

    @Test
    public void shouldAndIsNull() throws Exception {
        String sql = SQL + "WHERE [car:maker] IS NULL AND [car:model] IS NULL";
        logger.debug("\nSQL: {}", sql);

        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        QueryResult result = query.execute();

        printResults(result);

        Assert.assertEquals("Should contains rows", 1, result.getRows().getSize());
        List<String> actual = collectNames(result);
        Assert.assertTrue("Should contains NULL", actual.contains("NULL"));
    }

    @Test
    public void shouldPaging() throws Exception {
        String sql = SQL + "WHERE [car:maker] IN ('Aston Martin', '') ORDER BY [jcr:name]";
        logger.debug("\nSQL: {}", sql);

        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        query.setLimit(1);

        logger.debug("PAGE 1:");
        query.setOffset(0);
        QueryResult result = query.execute();

        printResults(result);

        Assert.assertEquals("Should contains rows", 1, result.getRows().getSize());
        List<String> actual = collectNames(result);
        Assert.assertTrue("Should contains Aston Martin", actual.contains("Aston Martin"));


        logger.debug("PAGE 2:");
        query.setOffset(1);
        result = query.execute();

        printResults(result);

        Assert.assertEquals("Should contains rows", 1, result.getRows().getSize());
        actual = collectNames(result);
        Assert.assertTrue("Should contains EMPTY", actual.contains("EMPTY"));
    }
}