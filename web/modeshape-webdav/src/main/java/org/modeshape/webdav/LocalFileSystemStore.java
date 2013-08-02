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
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.modeshape.common.i18n.TextI18n;
import org.modeshape.common.logging.Logger;
import org.modeshape.common.util.FileUtil;
import org.modeshape.common.util.IoUtil;
import org.modeshape.common.util.StringUtil;
import org.modeshape.webdav.exceptions.WebdavException;

/**
 * Reference Implementation of WebdavStore
 * 
 * @author joa
 * @author re
 * @author hchiorea@redhat.com
 */
public class LocalFileSystemStore implements IWebdavStore {

    private static Logger LOG = Logger.getLogger(LocalFileSystemStore.class);

    private static int BUF_SIZE = 65536;

    private File root = null;

    public LocalFileSystemStore( File root ) {
        this.root = root;
    }

    @Override
    public void destroy() {
    }

    @Override
    public ITransaction begin( Principal principal ) throws WebdavException {
        LOG.trace("LocalFileSystemStore.begin()");
        if (!root.exists()) {
            if (!root.mkdirs()) {
                throw new WebdavException("root path: " + root.getAbsolutePath() + " does not exist and could not be created");
            }
        }
        return null;
    }

    @Override
    public void checkAuthentication( ITransaction transaction ) throws SecurityException {
        LOG.trace("LocalFileSystemStore.checkAuthentication()");
        // do nothing

    }

    @Override
    public void commit( ITransaction transaction ) throws WebdavException {
        // do nothing
        LOG.trace("LocalFileSystemStore.commit()");
    }

    @Override
    public void rollback( ITransaction transaction ) throws WebdavException {
        // do nothing
        LOG.trace("LocalFileSystemStore.rollback()");

    }

    @Override
    public void createFolder( ITransaction transaction,
                              String uri ) throws WebdavException {
        LOG.trace("LocalFileSystemStore.createFolder(" + uri + ")");
        File file = new File(root, uri);
        if (!file.mkdir()) {
            throw new WebdavException("cannot create folder: " + uri);
        }
    }

    @Override
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

    @Override
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

    @Override
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

    @Override
    public void removeObject( ITransaction transaction,
                              String uri ) throws WebdavException {
        File file = new File(root, uri);
        boolean success = file.delete();
        LOG.trace("LocalFileSystemStore.removeObject(" + uri + ")=" + success);
        if (!success) {
            throw new WebdavException("cannot delete object: " + uri);
        }

    }

    @Override
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

    @Override
    public long getResourceLength( ITransaction transaction,
                                   String resourceUri ) throws WebdavException {
        LOG.trace("LocalFileSystemStore.getResourceLength(" + resourceUri + ")");
        File file = new File(root, resourceUri);
        return file.length();
    }

    @Override
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

    @Override
    public Map<String, String> setCustomProperties( ITransaction transaction,
                                                    String resourceUri,
                                                    Map<String, Object> propertiesToSet,
                                                    List<String> propertiesToRemove ) {
        LOG.trace("LocalFileSystemStore.setCustomProperties(" + resourceUri + ")");
        File propertiesFile = propertiesFileForResource(resourceUri);
        try {
            if (!propertiesFile.exists() && !propertiesToSet.isEmpty()) {
                propertiesFile.createNewFile();
            }

            if (!propertiesFile.exists()) {
                return null;
            }

            Map<String, Object> updatedProperties = readExistingProperties(propertiesFile);
            for (String propertyToRemove : propertiesToRemove) {
                updatedProperties.remove(propertyToRemove);
            }
            updatedProperties.putAll(propertiesToSet);

            if (updatedProperties.isEmpty()) {
                FileUtil.delete(propertiesFile);
            } else {
                writeProperties(propertiesFile, updatedProperties);
            }
        } catch (IOException e) {
            throw new WebdavException(e);
        }
        return null;
    }

    private File propertiesFileForResource( String resourceUri ) {
        File file = new File(root, resourceUri);
        if (!file.exists()) {
            throw new WebdavException(resourceUri + " does not represent an existing file or directory");
        }
        File propertiesFileParent = file.isFile() ? file.getParentFile() : file;
        if (!propertiesFileParent.canWrite()) {
            throw new WebdavException("Cannot write into the " + propertiesFileParent.getAbsolutePath()
                                      + " folder. Make sure that the FS permissions are correct");
        }
        String propertiesFileName = file.getName() + "_webdav.properties";
        return new File(propertiesFileParent, propertiesFileName);
    }

    private Map<String, Object> readExistingProperties( File propertiesFile ) throws IOException {
        Map<String, Object> properties = new HashMap<String, Object>();
        String fileContent = IoUtil.read(propertiesFile);
        if (StringUtil.isBlank(fileContent)) {
            return properties;
        }
        String[] keyValuePairs = fileContent.split(";");
        for (String keyValuePair : keyValuePairs) {
            String[] keyValue = keyValuePair.split("=");
            if (keyValue.length != 2) {
                continue;
            }
            properties.put(keyValue[0], valueFromString(keyValue[1]));
        }
        return properties;
    }

    private Object valueFromString( String value ) {
        if (value.startsWith("[") && value.endsWith("]")) {
            value = value.replaceAll("\\[", "").replaceAll("\\]", "");
            List<Object> array = new ArrayList<Object>();
            for (String element : value.split(",")) {
                array.add(valueFromString(element));
            }
            return array;
        }
        return value;
    }

    private void writeProperties( File propertiesFile,
                                  Map<String, Object> properties ) throws IOException {
        StringBuilder content = new StringBuilder();
        for (Iterator<Map.Entry<String, Object>> it = properties.entrySet().iterator(); it.hasNext();) {
            Map.Entry<String, Object> entry = it.next();
            content.append(entry.getKey()).append("=").append(valueToString(entry.getValue()));
            if (it.hasNext()) {
                content.append(";");
            }
        }

        IoUtil.write(content.toString(), propertiesFile);
    }

    private String valueToString( Object value ) {
        if (value instanceof List) {
            StringBuilder builder = new StringBuilder("[");
            for (Iterator<?> it = ((List<?>)value).iterator(); it.hasNext();) {
                builder.append(valueToString(it.next()));
                if (it.hasNext()) {
                    builder.append(",");
                }
            }
            builder.append("]");
            return builder.toString();
        }
        return value.toString();
    }

    @Override
    public Map<String, Object> getCustomProperties( ITransaction transaction,
                                                    String resourceUri ) {
        LOG.trace("LocalFileSystemStore.getCustomProperties(" + resourceUri + ")");
        try {
            File propertiesFile = propertiesFileForResource(resourceUri);
            if (propertiesFile.exists() && propertiesFile.canRead()) {
                return readExistingProperties(propertiesFile);
            }
            return Collections.emptyMap();
        } catch (IOException e) {
            throw new WebdavException(e);
        }
    }

    @Override
    public Map<String, String> getCustomNamespaces( ITransaction transaction,
                                                    String resourceUri ) {
        // the default FS based implementation does not use custom namespaces
        return Collections.emptyMap();
    }
}
