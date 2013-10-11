package org.modeshape.jcr;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.NodeIterator;

/*
 *	Test that data not changed during changing node types
 */

public class MigrateDataTest3 extends SingleUseAbstractTest
{
	private static final Logger LOG = LoggerFactory.getLogger(MigrateDataTest.class);

	// Step 1: Load Repository 1 and fill it with test data like:
	// types1
	//		subtypes1
	//		subtypes2
	// types2
	//		subtypes1
	//		subtypes2
	private void stepOne() throws Exception
	{
		LOG.info("--------- Repository 1 -------------");
		startRepositoryWithConfiguration(resourceStream("config/migate-test3-repository1.json"));

		Node root = session.getRootNode();
		fillData(root);
		printData(root);
		session.save();

		stopRepository();
	}

	// Step 2: Trying to load Repository 2 with changed CND file
	private void stepTwo() throws Exception
	{
		LOG.info("--------- Repository 2 -------------");
		startRepositoryWithConfiguration(resourceStream("config/migate-test3-repository2.json"));

		Node root = session.getRootNode();
		printData(root);

		stopRepository();
	}

	// Step 3: Again trying to load Repository 1
	private void stepThree() throws Exception
	{
		LOG.info("--------- Repository 1 -------------");
		startRepositoryWithConfiguration(resourceStream("config/migate-test3-repository1.json"));

		Node root = session.getRootNode();
		printData(root);

		stopRepository();
	}

	@Test
	public void reproduceSteps() throws Exception
	{
		stepOne();
		stepTwo();
		stepThree();
	}

	private void fillData(Node root) throws Exception
	{
		Node node;

		node = root.addNode("types1", "mode:type1");
		node.addNode("subtypes1", "mode:type1");
		node.addNode("subtypes2", "mode:type2");

		node = root.addNode("types2", "mode:type2");
		node.addNode("subtypes1", "mode:type1");
		node.addNode("subtypes2", "mode:type2");
	}

	private void printData(Node root) throws Exception
	{
		LOG.info(root.getName());

		Node i;
		NodeIterator node = root.getNodes();
		while (node.hasNext())
		{
			i = node.nextNode();
			LOG.info("\t"+i.getName());

			Node j;
			NodeIterator subnode = i.getNodes();
			while(subnode.hasNext())
			{
				j = subnode.nextNode();
				LOG.info("\t\t"+j.getName());
			}
		}
	}
}