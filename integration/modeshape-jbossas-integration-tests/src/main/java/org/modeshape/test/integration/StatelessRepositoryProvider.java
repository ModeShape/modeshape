package org.modeshape.test.integration;

import javax.ejb.Stateless;

/**
 * A stateless EJB that accesses a Repository and creates a JCR Session a variety of ways.
 * <p>
 * This class extends the {@link RepositoryProvider}, which has all the methods for obtaining repositories and using the sessions.
 * </p>
 */
@Stateless
public class StatelessRepositoryProvider extends RepositoryProvider {
}
