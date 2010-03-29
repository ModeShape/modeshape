package org.modeshape.web.jcr.spi;

import javax.jcr.RepositoryException;

/**
 * Exception thrown when an operation attempts to access a repository that does not exist.
 */
public class NoSuchRepositoryException extends RepositoryException {

    private static final long serialVersionUID = 1L;

    public NoSuchRepositoryException() {
        super();
    }

    public NoSuchRepositoryException( String message,
                                      Throwable rootCause ) {
        super(message, rootCause);
    }

    public NoSuchRepositoryException( String message ) {
        super(message);
    }

    public NoSuchRepositoryException( Throwable rootCause ) {
        super(rootCause);
    }

}
