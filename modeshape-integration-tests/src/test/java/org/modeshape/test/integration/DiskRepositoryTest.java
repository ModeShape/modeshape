/*
 * ModeShape (http://www.modeshape.org)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of
 * individual contributors.
 *
 * ModeShape is free software. Unless otherwise indicated, all code in ModeShape
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * ModeShape is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.modeshape.test.integration;

import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.modeshape.common.FixFor;
import org.modeshape.common.util.FileUtil;
import org.modeshape.test.ModeShapeSingleUseTest;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.UUID;

/**
 * Integration test for a JCR repository based on a disk source.
 *
 * @author Horia Chiorean
 */
public class DiskRepositoryTest extends ModeShapeSingleUseTest {
    private static final String REPOSITORY_NAME = "Repo";
    private static final String DEFAULT_WS_NAME = "default";
    private static final String OTHER_WS_NAME = "otherWorkspace";

    @Override
    public void beforeEach() throws Exception {
        File repositoryFolder = new File("target/database/ConfigurationTest/files");
        if (repositoryFolder.exists()) {
            assertTrue(FileUtil.delete(repositoryFolder));
        }
        super.beforeEach();
        engine();
    }

    @Override
    public void afterEach() throws Exception {
        closeSessions();    
        super.afterEach();        
    }

    @FixFor("MODE-1298")
    @Test
    public void cloneBinaryContentIntoAnotherWorkspaceAndModifyOriginal() throws Exception {
        //upload documents into default ws
        String firstDocumentPath = uploadDocument("diskstore/duke.png");
        String secondDocumentPath = uploadDocument("diskstore/nasa.pdf");
        
        //clone the first document & check
        cloneDocument(firstDocumentPath);
        assertClonedContent(firstDocumentPath, true);
        
        //clone 2nd document & check
        cloneDocument(secondDocumentPath);
        assertClonedContent(secondDocumentPath, true);
        
        //change first content & check
        changeOrDeleteContent(firstDocumentPath, "dummyContent");
        assertClonedContent(firstDocumentPath, false);
        
        //remove second document & check
        changeOrDeleteContent(secondDocumentPath, null);
        assertClonedContent(secondDocumentPath, false);

        //restart repository
        closeSessions();
        stopEngine();
        engine();
        
        //check again the last state
        assertClonedContent(firstDocumentPath, false);
        assertClonedContent(secondDocumentPath, false);
    }

    private void cloneDocument( String documentPath) throws Exception{
        Session otherSession = sessionTo(REPOSITORY_NAME, OTHER_WS_NAME);
        otherSession.getWorkspace().clone(DEFAULT_WS_NAME, documentPath, documentPath, true);
        otherSession.logout();
    }
    
    private void changeOrDeleteContent( String documentPath,
                                        String dummyContent ) throws Exception {
        Session defaultSession = session();
        if (dummyContent != null) {
            Node document = defaultSession.getNode(documentPath);
            Binary updatedDocumentContent = defaultSession.getValueFactory().createBinary(new ByteArrayInputStream(
                    dummyContent.getBytes()));            
            document.setProperty("jcr:data", updatedDocumentContent);
        }
        else {
            defaultSession.removeItem(documentPath);
        }
        defaultSession.save();    
    }

    private String uploadDocument(String path) throws Exception {
        Session defaultSession = session();
        Node document = defaultSession.getRootNode().addNode("document_" + UUID.randomUUID().toString());
        Binary documentContent = defaultSession.getValueFactory().createBinary(getClass().getClassLoader()
                                                                                       .getResourceAsStream(path));
        document.setProperty("jcr:data", documentContent);
        String documentPath = document.getPath();
        defaultSession.save();
        defaultSession.logout();
        return documentPath;
    }
    
    private void assertClonedContent( String documentAbsPath, boolean shouldBeSame) throws RepositoryException {
        long originalSize = 0;
        Session defaultSession = sessionTo(REPOSITORY_NAME, DEFAULT_WS_NAME);
        try {
            Node originalDocument = defaultSession.getNode(documentAbsPath);
            Binary originalBinaryValue = originalDocument.getProperty("jcr:data").getBinary();
            originalSize = originalBinaryValue.getSize();
        } catch (PathNotFoundException e) {
            //ignore, it means the original has been removed
        }
        finally {
            defaultSession.logout();
        }

        Session otherSession = sessionTo(REPOSITORY_NAME, OTHER_WS_NAME);
        Node clonedDocument = otherSession.getNode(documentAbsPath);
        Binary clonedBinaryValue = clonedDocument.getProperty("jcr:data").getBinary();
        long clonedSize = clonedBinaryValue.getSize();
        otherSession.logout();

        if (shouldBeSame) {
            assertEquals(originalSize, clonedSize);
        }
        else {
            assertTrue(originalSize != clonedSize);
        }
    }

    @Override
    protected String getPathToDefaultConfiguration() {
        return "config/configRepositoryForDiskStorage.xml";
    }
}
