package org.modeshape.webdav.exceptions;

public class LockFailedException extends WebdavException {

    private static final long serialVersionUID = 1L;

    public LockFailedException() {
        super();
    }

    public LockFailedException( String message ) {
        super(message);
    }

    public LockFailedException( String message,
                                Throwable cause ) {
        super(message, cause);
    }

    public LockFailedException( Throwable cause ) {
        super(cause);
    }
}
