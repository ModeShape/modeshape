/*
 *
 */
package org.jboss.dna.maven;

/**
 * An configuration or system failure exception.
 * @author Randall Hauch
 */
public class MavenRepositoryException extends RuntimeException {

    /**
     * 
     */
    public MavenRepositoryException() {
        super();

    }

    /**
     * @param message
     * @param cause
     */
    public MavenRepositoryException( String message, Throwable cause ) {
        super(message, cause);

    }

    /**
     * @param message
     */
    public MavenRepositoryException( String message ) {
        super(message);

    }

    /**
     * @param cause
     */
    public MavenRepositoryException( Throwable cause ) {
        super(cause);

    }

}
