package org.modeshape.jcr;

import org.junit.Test;

/*
	Test without notifications:printMailNotification
	and notifications:baseNotification in Repository 3
 */

public class MigrateDataTest2 extends SingleUseAbstractTest
{
	@Test
	public void shouldLoadRepositories() throws Exception
	{
		// Successes
		startRepositoryWithConfiguration(resourceStream("config/migate-test-repository1.json"));
		stopRepository();

		// Successes
		startRepositoryWithConfiguration(resourceStream("config/migate-test-repository3.json"));
		stopRepository();

		// Successes
		startRepositoryWithConfiguration(resourceStream("config/migate-test-repository1.json"));
		stopRepository();
	}
}