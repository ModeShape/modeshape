package org.modeshape.jcr;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;

import java.io.InputStream;

public class ImportExportPersistentTest extends ImportExportTest {

	@Override
	public void startRepository() throws Exception {
		InputStream configInputStream = getClass().getClassLoader()
				.getResourceAsStream("config/repo-h2.json");
		assertThat(configInputStream, is(notNullValue()));
		config = RepositoryConfiguration.read(configInputStream, REPO_NAME);
		startRepositoryWithConfiguration(config);
	}

}
