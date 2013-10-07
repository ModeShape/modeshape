package org.modeshape.jcr;

import org.junit.Test;


public class MigrateDataTest extends SingleUseAbstractTest
{
	@Test
	public void shouldLoadRepositories() throws Exception
	{
		// Successes
		startRepositoryWithConfiguration(resourceStream("config/migate-test-repository1.json"));
		stopRepository();

		// Successes
		startRepositoryWithConfiguration(resourceStream("config/migate-test-repository2.json"));
		stopRepository();

		// Successes
		startRepositoryWithConfiguration(resourceStream("config/migate-test-repository1.json"));
		stopRepository();
	}
}