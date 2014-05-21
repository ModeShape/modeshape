package org.modeshape.jcr;

import junit.framework.Assert;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import java.util.ArrayList;
import java.util.List;

/**
 * See MODE-2174: JCR Query in Modeshape returns no result when empty string used in comparison
 * @author: vadym.karko
 * @since: 3/21/14 12:35 PM
 */
public class QueryEmptyStringTest extends MultiUseAbstractTest
{
	private static final String SQL = "SELECT [jcr:name], [car:model], [car:maker] FROM [car:Car] ";

	@BeforeClass
	public static void setUp() throws Exception
	{
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
		 * jcr:name			|	car:maker		|	car:model
		 * ---------------------------------------------------
		 * 'Aston Martin'	|	'Aston Martin'	|	'DB9'
		 * 'Infiniti'		|	'Infiniti'		|	null
		 * 'EMPTY'			|	''				|	null
		 * 'NULL'			|	null			|	null
		 */

		session.save();
	}

	@AfterClass
	public static void tearDown() throws Exception
	{
		stopRepository();
	}

	@Before
	public void before() throws RepositoryException
	{
		System.out.println("\nBefore:");

		Query query = session.getWorkspace().getQueryManager().createQuery(SQL, Query.JCR_SQL2);
		QueryResult result = query.execute();

		printResults(result);
	}

	private List<String> collectNames(QueryResult result) throws RepositoryException
	{
		NodeIterator iterator = result.getNodes();
		List<String> actual = new ArrayList<String>();
		while (iterator.hasNext()) actual.add(iterator.nextNode().getName());

		return actual;
	}

	@Override
	protected void printResults(QueryResult results)
	{
		print = true;
		super.printResults(results);
	}


	@Test
	public void shouldEqualsEmpty() throws Exception
	{
		String sql = SQL + "WHERE [car:maker] = ''";
		System.out.println("\nSQL: " + sql);

		Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
		QueryResult result = query.execute();

		printResults(result);

		Assert.assertEquals("Should contains rows", 1, result.getRows().getSize());
		List<String> actual = collectNames(result);
		Assert.assertTrue("Should contains EMPTY", actual.contains("EMPTY"));
	}

	@Test
	public void shouldIsNotNull() throws Exception
	{
		String sql = SQL + "WHERE [car:maker] IS NOT NULL";
		System.out.println("\nSQL: " + sql +"\n");

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
	public void shouldIsNull() throws Exception
	{
		String sql = SQL + "WHERE [car:maker] IS NULL";
		System.out.println("\nSQL: " + sql +"\n");

		Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
		QueryResult result = query.execute();

		printResults(result);

		Assert.assertEquals(1, result.getRows().getSize());
		List<String> actual = collectNames(result);
		Assert.assertTrue("Should contains NULL", actual.contains("NULL"));
	}

	@Test
	public void shouldLengthEqualsZero() throws Exception
	{
		String sql = SQL + "WHERE LENGTH([car:maker]) = 0";
		System.out.println("\nSQL: " + sql +"\n");

		Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
		QueryResult result = query.execute();

		printResults(result);

		Assert.assertEquals(1, result.getRows().getSize());
		List<String> actual = collectNames(result);
		Assert.assertTrue("Should contains EMPTY", actual.contains("EMPTY"));
	}

	@Test
	public void shouldLikeEmpty() throws Exception
	{
		String sql = SQL + "WHERE [car:maker] LIKE ''";
		System.out.println("\nSQL: " + sql +"\n");

		Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
		QueryResult result = query.execute();

		printResults(result);

		Assert.assertEquals(1, result.getRows().getSize());
		List<String> actual = collectNames(result);
		Assert.assertTrue("Should contains EMPTY", actual.contains("EMPTY"));
	}

	@Test
	public void shouldInEmpty() throws Exception
	{
		String sql = SQL + "WHERE [car:maker] IN ('')";
		System.out.println("\nSQL: " + sql +"\n");

		Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
		QueryResult result = query.execute();

		printResults(result);

		Assert.assertEquals(1, result.getRows().getSize());
		List<String> actual = collectNames(result);
		Assert.assertTrue("Should contains EMPTY", actual.contains("EMPTY"));
	}

	@Test
	public void shouldInEmptyAndString() throws Exception
	{
		String sql = SQL + "WHERE [car:maker] IN ('Aston Martin', '')";
		System.out.println("\nSQL: " + sql +"\n");

		Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
		QueryResult result = query.execute();

		printResults(result);


		Assert.assertEquals("Should contains rows", 2, result.getRows().getSize());
		List<String> actual = collectNames(result);
		Assert.assertTrue("Should contains EMPTY", actual.contains("EMPTY"));
		Assert.assertTrue("Should contains Aston Martin", actual.contains("Aston Martin"));
	}

	@Test
	public void shouldOrEqualsEmpty() throws Exception
	{
		String sql = SQL + "WHERE [car:maker] = '' OR [car:maker] = 'Infiniti'";
		System.out.println("\nSQL: " + sql +"\n");

		Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
		QueryResult result = query.execute();

		printResults(result);


		Assert.assertEquals("Should contains rows", 2, result.getRows().getSize());
		List<String> actual = collectNames(result);
		Assert.assertTrue("Should contains EMPTY", actual.contains("EMPTY"));
		Assert.assertTrue("Should contains Infiniti", actual.contains("Infiniti"));
	}

	@Test
	public void shouldOrIsNull() throws Exception
	{
		String sql = SQL + "WHERE [car:maker] IS NULL OR [car:model] = 'DB9'";
		System.out.println("\nSQL: " + sql +"\n");

		Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
		QueryResult result = query.execute();

		printResults(result);


		Assert.assertEquals("Should contains rows", 2, result.getRows().getSize());
		List<String> actual = collectNames(result);
		Assert.assertTrue("Should contains Aston Martin", actual.contains("Aston Martin"));
		Assert.assertTrue("Should contains NULL", actual.contains("NULL"));
	}

	@Test
	public void shouldOrIsNotNull() throws Exception
	{
		String sql = SQL + "WHERE [car:maker] IS NOT NULL OR [car:model] = 'DB9'";
		System.out.println("\nSQL: " + sql +"\n");

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
	public void shouldAndEqualsEmpty() throws Exception
	{
		String sql = SQL + "WHERE [car:maker] = '' AND [car:model] IS NULL";
		System.out.println("\nSQL: " + sql +"\n");

		Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
		QueryResult result = query.execute();

		printResults(result);

		Assert.assertEquals("Should contains rows", 1, result.getRows().getSize());
		List<String> actual = collectNames(result);
		Assert.assertTrue("Should contains EMPTY", actual.contains("EMPTY"));
	}

	@Test
	public void shouldAndIsNotNull() throws Exception
	{
		String sql = SQL + "WHERE [car:maker] IS NOT NULL AND [car:model] = 'DB9'";
		System.out.println("\nSQL: " + sql +"\n");

		Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
		QueryResult result = query.execute();

		printResults(result);

		Assert.assertEquals("Should contains rows", 1, result.getRows().getSize());
		List<String> actual = collectNames(result);
		Assert.assertTrue("Should contains Aston Martin", actual.contains("Aston Martin"));
	}

	@Test
	public void shouldAndIsNull() throws Exception
	{
		String sql = SQL + "WHERE [car:maker] IS NULL AND [car:model] IS NULL";
		System.out.println("\nSQL: " + sql +"\n");

		Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
		QueryResult result = query.execute();

		printResults(result);

		Assert.assertEquals("Should contains rows", 1, result.getRows().getSize());
		List<String> actual = collectNames(result);
		Assert.assertTrue("Should contains NULL", actual.contains("NULL"));
	}

	@Test
	public void shouldPaging() throws Exception
	{
		String sql = SQL + "WHERE [car:maker] IN ('Aston Martin', '') ORDER BY [jcr:name]";
		System.out.println("\nSQL: " + sql +"\n");

		Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
		query.setLimit(1);

		System.out.println("PAGE 1:");
		query.setOffset(0);
		QueryResult result = query.execute();

		printResults(result);

		Assert.assertEquals("Should contains rows", 1, result.getRows().getSize());
		List<String> actual = collectNames(result);
		Assert.assertTrue("Should contains Aston Martin", actual.contains("Aston Martin"));


		System.out.println("PAGE 2:");
		query.setOffset(1);
		result = query.execute();

		printResults(result);

		Assert.assertEquals("Should contains rows", 1, result.getRows().getSize());
		actual = collectNames(result);
		Assert.assertTrue("Should contains EMPTY", actual.contains("EMPTY"));
	}
}