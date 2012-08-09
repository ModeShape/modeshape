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

package org.modeshape.web.jcr.rest.handler;

import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.modeshape.common.util.CheckArg;
import org.modeshape.common.util.StringUtil;
import org.modeshape.web.jcr.rest.model.RestProperty;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author Horia Chiorean
 */
public final class RestBinaryHandler extends AbstractHandler {

    protected static final String DEFAULT_CONTENT_DISPOSITION = "attachment;filename=";


    public Property getBinaryProperty( HttpServletRequest request,
                                       String repositoryName,
                                       String workspaceName,
                                       String binaryPath ) throws RepositoryException {
        Session session = getSession(request, repositoryName, workspaceName);
        return session.getProperty(binaryPath);
    }

    public String getDefaultContentDisposition( Property binaryProperty ) throws RepositoryException {
        Node parentNode = getParentNode(binaryProperty);
        String parentName = parentNode.getName();
        if (StringUtil.isBlank(parentName)) {
            parentName = "binary";
        }
        return DEFAULT_CONTENT_DISPOSITION + parentName;
    }

    public String getDefaultMimeType( Property binaryProperty ) throws RepositoryException {
        try {
            Binary binary = binaryProperty.getBinary();
            return binary instanceof org.modeshape.jcr.api.Binary ? ((org.modeshape.jcr.api.Binary) binary).getMimeType() : MediaType.APPLICATION_OCTET_STREAM;
        } catch (IOException e) {
            logger.warn("Cannot determine default mime-type", e);
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    public Response updateBinary( HttpServletRequest request,
                                  String repositoryName,
                                  String workspaceName,
                                  String path,
                                  InputStream binaryStream,
                                  boolean allowCreation ) throws RepositoryException {
        //TODO author=Horia Chiorean date=8/9/12 description=We don't have a way (without changing the API) to set the mime-type on a binary
        CheckArg.isNotNull(binaryStream, "request body");

        int lastSlashInd = path.lastIndexOf('/');
        String parentPath = lastSlashInd == -1 ? "/" : "/" + path.substring(0, lastSlashInd);
        Session session = getSession(request, repositoryName, workspaceName);
        Node parent = (Node)itemAtPath(parentPath, session);

        String propertyName = lastSlashInd == -1 ? path : path.substring(lastSlashInd + 1);
        try {
            Property binaryProperty = null;
            try {
                binaryProperty = parent.getProperty(propertyName);
                //edit an existing property
                Binary binary = session.getValueFactory().createBinary(binaryStream);
                binaryProperty.setValue(binary);
            } catch (PathNotFoundException e) {
                if (!allowCreation) {
                    return Response.status(Response.Status.NOT_FOUND).build();
                }
                // create a new binary property
                Binary binary = session.getValueFactory().createBinary(binaryStream);
                binaryProperty = parent.setProperty(propertyName, binary);
            }
            session.save();
            RestProperty restItem = (RestProperty)createRestItem(request, 0, session, binaryProperty);
            return Response.ok().entity(restItem).build();
        } finally {
            try {
                binaryStream.close();
            } catch (IOException e) {
                logger.error("Cannot close binary stream", e);
            }
        }
    }
}
