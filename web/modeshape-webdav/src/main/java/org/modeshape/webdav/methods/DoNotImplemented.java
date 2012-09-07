package org.modeshape.webdav.methods;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.modeshape.common.logging.Logger;
import org.modeshape.webdav.IMethodExecutor;
import org.modeshape.webdav.ITransaction;
import org.modeshape.webdav.WebdavStatus;
import java.io.IOException;

public class DoNotImplemented implements IMethodExecutor {

    private static Logger LOG = Logger.getLogger(DoNotImplemented.class);

    private final boolean readOnly;

    public DoNotImplemented( boolean readOnly ) {
        this.readOnly = readOnly;
    }

    public void execute( ITransaction transaction,
                         HttpServletRequest req,
                         HttpServletResponse resp ) throws IOException {
        LOG.trace("-- " + req.getMethod());
        if (readOnly) {
            resp.sendError(WebdavStatus.SC_FORBIDDEN);
        } else {
            resp.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED);
        }
    }
}
