/*
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.modeshape.webdav;

import org.modeshape.common.i18n.TextI18n;
import org.modeshape.common.logging.Logger;
import org.modeshape.webdav.exceptions.WebdavException;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Reference Implementation of WebdavStore
 *
 * @author joa
 * @author re
 */
public class LocalFileSystemStore implements IWebdavStore {

    private static Logger LOG = Logger.getLogger(LocalFileSystemStore.class);

    private static int BUF_SIZE = 65536;

    private File root = null;

    public LocalFileSystemStore( File root ) {
        this.root = root;
    }

    public void destroy() {
    }

    public ITransaction begin( Principal principal ) throws WebdavException {
        LOG.trace("LocalFileSystemStore.begin()");
        if (!root.exists()) {
            if (!root.mkdirs()) {
                throw new WebdavException("root path: " + root.getAbsolutePath() + " does not exist and could not be created");
            }
        }
        return null;
    }

    public void checkAuthentication( ITransaction transaction ) throws SecurityException {
        LOG.trace("LocalFileSystemStore.checkAuthentication()");
        // do nothing

    }

    public void commit( ITransaction transaction ) throws WebdavException {
        // do nothing
        LOG.trace("LocalFileSystemStore.commit()");
    }

    public void rollback( ITransaction transaction ) throws WebdavException {
        // do nothing
        LOG.trace("LocalFileSystemStore.rollback()");

    }

    public void createFolder( ITransaction transaction,
                              String uri ) throws WebdavException {
        LOG.trace("LocalFileSystemStore.createFolder(" + uri + ")");
        File file = new File(root, uri);
        if (!file.mkdir()) {
            throw new WebdavException("cannot create folder: " + uri);
        }
    }

    public void createResource( ITransaction transaction,
                                String uri ) throws WebdavException {
        LOG.trace("LocalFileSystemStore.createResource(" + uri + ")");
        File file = new File(root, uri);
        try {
            if (!file.createNewFile()) {
                throw new WebdavException("cannot create file: " + uri);
            }
        } catch (IOException e) {
            LOG.error(new TextI18n("LocalFileSystemStore.createResource(" + uri + ") failed"));
            throw new WebdavException(e);
        }
    }

    public long setResourceContent( ITransaction transaction,
                                    String uri,
                                    InputStream is,
                                    String contentType,
                                    String characterEncoding ) throws WebdavException {

        LOG.trace("LocalFileSystemStore.setResourceContent(" + uri + ")");
        File file = new File(root, uri);
        try {
            OutputStream os = new BufferedOutputStream(new FileOutputStream(file), BUF_SIZE);
            try {
                int read;
                byte[] copyBuffer = new byte[BUF_SIZE];

                while ((read = is.read(copyBuffer, 0, copyBuffer.length)) != -1) {
                    os.write(copyBuffer, 0, read);
                }
            } finally {
                try {
                    is.close();
                } finally {
                    os.close();
                }
            }
        } catch (IOException e) {
            LOG.error(new TextI18n("LocalFileSystemStore.setResourceContent(" + uri + ") failed"));
            throw new WebdavException(e);
        }
        long length = -1;

        try {
            length = file.length();
        } catch (SecurityException e) {
            LOG.error(new TextI18n("LocalFileSystemStore.setResourceContent(" + uri + ") failed" + "\nCan't get file.length"));
        }

        return length;
    }

    public String[] getChildrenNames( ITransaction transaction,
                                      String uri ) throws WebdavException {
        LOG.trace("LocalFileSystemStore.getChildrenNames(" + uri + ")");
        File file = new File(root, uri);
        String[] childrenNames = null;
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            List<String> childList = new ArrayList<String>();
            String name = null;
            for (int i = 0; i < children.length; i++) {
                name = children[i].getName();
                childList.add(name);
                LOG.trace("Child " + i + ": " + name);
            }
            childrenNames = new String[childList.size()];
            childrenNames = childList.toArray(childrenNames);
        }
        return childrenNames;
    }

    public void removeObject( ITransaction transaction,
                              String uri ) throws WebdavException {
        File file = new File(root, uri);
        boolean success = file.delete();
        LOG.trace("LocalFileSystemStore.removeObject(" + uri + ")=" + success);
        if (!success) {
            throw new WebdavException("cannot delete object: " + uri);
        }

    }

    public InputStream getResourceContent( ITransaction transaction,
                                           String uri ) throws WebdavException {
        LOG.trace("LocalFileSystemStore.getResourceContent(" + uri + ")");
        File file = new File(root, uri);

        InputStream in;
        try {
            in = new BufferedInputStream(new FileInputStream(file));
        } catch (IOException e) {
            LOG.error(new TextI18n("LocalFileSystemStore.getResourceContent(" + uri + ") failed"));
            throw new WebdavException(e);
        }
        return in;
    }

    public long getResourceLength( ITransaction transaction,
                                   String resourceUri ) throws WebdavException {
        LOG.trace("LocalFileSystemStore.getResourceLength(" + resourceUri + ")");
        File file = new File(root, resourceUri);
        return file.length();
    }

    public StoredObject getStoredObject( ITransaction transaction,
                                         String uri ) {

        StoredObject so = null;

        File file = new File(root, uri);
        if (file.exists()) {
            so = new StoredObject();
            so.setFolder(file.isDirectory());
            so.setLastModified(new Date(file.lastModified()));
            so.setCreationDate(new Date(file.lastModified()));
            so.setResourceLength(getResourceLength(transaction, uri));
        }

        return so;
    }

}
