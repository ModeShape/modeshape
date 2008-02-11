package org.jboss.dna.common.util;

import org.jboss.dna.common.util.LogContext;
import org.jboss.dna.common.util.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;


public class LogContextTest {

	private Logger logger;
	
	@Before
	public void beforeEach() {
		logger = Logger.getLogger(LoggerTest.class);
	}
	
	@After
	public void afterEach(){
		LogContext.clear();
	}
	
	@Test
	public void shouldAcceptValidKeys(){
		LogContext.set("username", "jsmith");
		logger.info("tracking activity for username");
		logger.info("A second log message");
	}

}
