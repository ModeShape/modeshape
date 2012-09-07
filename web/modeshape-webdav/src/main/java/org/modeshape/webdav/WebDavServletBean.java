package org.modeshape.webdav;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.modeshape.common.i18n.TextI18n;
import org.modeshape.common.logging.Logger;
import org.modeshape.webdav.exceptions.UnauthenticatedException;
import org.modeshape.webdav.exceptions.WebdavException;
import org.modeshape.webdav.locking.ResourceLocks;
import org.modeshape.webdav.methods.DoCopy;
import org.modeshape.webdav.methods.DoDelete;
import org.modeshape.webdav.methods.DoGet;
import org.modeshape.webdav.methods.DoHead;
import org.modeshape.webdav.methods.DoLock;
import org.modeshape.webdav.methods.DoMkcol;
import org.modeshape.webdav.methods.DoMove;
import org.modeshape.webdav.methods.DoNotImplemented;
import org.modeshape.webdav.methods.DoOptions;
import org.modeshape.webdav.methods.DoPropfind;
import org.modeshape.webdav.methods.DoProppatch;
import org.modeshape.webdav.methods.DoPut;
import org.modeshape.webdav.methods.DoUnlock;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

public class WebDavServletBean extends HttpServlet {

    private static Logger LOG = Logger.getLogger(WebDavServletBean.class);

    /**
     * MD5 message digest provider.
     */
    protected static MessageDigest MD5_HELPER;

    private static final boolean READ_ONLY = false;
    protected final ResourceLocks resLocks;
    protected IWebdavStore store;
    private Map<String, IMethodExecutor> methodMap = new HashMap<String, IMethodExecutor>();

    public WebDavServletBean() {
        resLocks = new ResourceLocks();

        try {
            MD5_HELPER = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public void init( IWebdavStore store,
                      String dftIndexFile,
                      String insteadOf404,
                      int nocontentLenghHeaders,
                      boolean lazyFolderCreationOnPut ) throws ServletException {

        this.store = store;

        IMimeTyper mimeTyper = new IMimeTyper() {
            public String getMimeType( ITransaction transaction,
                                       String path ) {
                String retVal = WebDavServletBean.this.store.getStoredObject(transaction, path).getMimeType();
                if (retVal == null) {
                    retVal = getServletContext().getMimeType(path);
                }
                return retVal;
            }
        };

        register("GET", new DoGet(store, dftIndexFile, insteadOf404, resLocks, mimeTyper, nocontentLenghHeaders));
        register("HEAD", new DoHead(store, dftIndexFile, insteadOf404, resLocks, mimeTyper, nocontentLenghHeaders));
        DoDelete doDelete = (DoDelete)register("DELETE", new DoDelete(store, resLocks, READ_ONLY));
        DoCopy doCopy = (DoCopy)register("COPY", new DoCopy(store, resLocks, doDelete, READ_ONLY));
        register("LOCK", new DoLock(store, resLocks, READ_ONLY));
        register("UNLOCK", new DoUnlock(store, resLocks, READ_ONLY));
        register("MOVE", new DoMove(resLocks, doDelete, doCopy, READ_ONLY));
        register("MKCOL", new DoMkcol(store, resLocks, READ_ONLY));
        register("OPTIONS", new DoOptions(store, resLocks));
        register("PUT", new DoPut(store, resLocks, READ_ONLY, lazyFolderCreationOnPut));
        register("PROPFIND", new DoPropfind(store, resLocks, mimeTyper));
        register("PROPPATCH", new DoProppatch(store, resLocks, READ_ONLY));
        register("*NO*IMPL*", new DoNotImplemented(READ_ONLY));
    }

    @Override
    public void destroy() {
        if (store != null) {
            store.destroy();
        }
        super.destroy();
    }

    protected IMethodExecutor register( String methodName,
                                        IMethodExecutor method ) {
        methodMap.put(methodName, method);
        return method;
    }

    /**
     * Handles the special WebDAV methods.
     */
    @Override
    protected void service( HttpServletRequest req,
                            HttpServletResponse resp ) throws ServletException, IOException {

        String methodName = req.getMethod();
        ITransaction transaction = null;
        boolean needRollback = false;

        if (LOG.isTraceEnabled()) {
            debugRequest(methodName, req);
        }

        try {
            Principal userPrincipal = req.getUserPrincipal();
            transaction = store.begin(userPrincipal);
            needRollback = true;
            store.checkAuthentication(transaction);
            resp.setStatus(WebdavStatus.SC_OK);

            try {
                IMethodExecutor methodExecutor = methodMap.get(methodName);
                if (methodExecutor == null) {
                    methodExecutor = methodMap.get("*NO*IMPL*");
                }

                methodExecutor.execute(transaction, req, resp);

                store.commit(transaction);
                /** Clear not consumed data
                 *
                 * Clear input stream if available otherwise later access
                 * include current input.  These cases occure if the client
                 * sends a request with body to an not existing resource.
                 */
                if (req.getContentLength() != 0 && req.getInputStream().available() > 0) {
                    if (LOG.isTraceEnabled()) {
                        LOG.trace("Clear not consumed data!");
                    }
                    while (req.getInputStream().available() > 0) {
                        req.getInputStream().read();
                    }
                }
                needRollback = false;
            } catch (IOException e) {
                LOG.error(e, new TextI18n("IOException"));
                resp.sendError(WebdavStatus.SC_INTERNAL_SERVER_ERROR);
                store.rollback(transaction);
                throw new ServletException(e);
            }

        } catch (UnauthenticatedException e) {
            resp.sendError(WebdavStatus.SC_FORBIDDEN);
        } catch (WebdavException e) {
            LOG.error(e, new TextI18n("WebdavException"));
            throw new ServletException(e);
        } catch (Exception e) {
            LOG.error(e, new TextI18n("Exception"));
        } finally {
            if (needRollback) {
                store.rollback(transaction);
            }
        }

    }

    private void debugRequest( String methodName,
                               HttpServletRequest req ) {
        LOG.trace("-----------");
        LOG.trace("WebdavServlet\n request: methodName = " + methodName);
        LOG.trace("time: " + System.currentTimeMillis());
        LOG.trace("path: " + req.getRequestURI());
        LOG.trace("-----------");
        Enumeration<?> e = req.getHeaderNames();
        while (e.hasMoreElements()) {
            String s = (String)e.nextElement();
            LOG.trace("header: " + s + " " + req.getHeader(s));
        }
        e = req.getAttributeNames();
        while (e.hasMoreElements()) {
            String s = (String)e.nextElement();
            LOG.trace("attribute: " + s + " " + req.getAttribute(s));
        }
        e = req.getParameterNames();
        while (e.hasMoreElements()) {
            String s = (String)e.nextElement();
            LOG.trace("parameter: " + s + " " + req.getParameter(s));
        }
    }

}
