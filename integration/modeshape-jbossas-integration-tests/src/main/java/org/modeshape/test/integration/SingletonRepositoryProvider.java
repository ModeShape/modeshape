package org.modeshape.test.integration;

import javax.ejb.Singleton;

/**
 * A singleton EJB that accesses a Repository and creates a JCR Session a variety of ways.
 * <p>
 * This class extends the {@link RepositoryProvider}, which has all the methods for obtaining repositories and using the sessions.
 * </p>
 */
@Singleton
public class SingletonRepositoryProvider extends RepositoryProvider {
}
