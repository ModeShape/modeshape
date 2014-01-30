/*
 * ModeShape (http://www.modeshape.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.modeshape.web.jcr.rest.handler;

import java.io.IOException;
import java.io.InputStream;
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

/**
 * Class which handles incoming requests related to {@link Binary binary values}
 *
 * @author Horia Chiorean
 */
public final class RestBinaryHandler extends AbstractHandler {

    /**
     * The default content disposition prefix, used when serving binary content.
     */
    public static final String DEFAULT_CONTENT_DISPOSITION_PREFIX = "attachment;filename=";
    private static final String DEFAULT_MIME_TYPE = MediaType.APPLICATION_OCTET_STREAM;

    /**
     * Returns a binary {@link Property} for the given repository, workspace and path.
     *
     * @param request a non-null {@link HttpServletRequest} request
     * @param repositoryName a non-null {@link String} representing the name of a repository.
     * @param workspaceName a non-null {@link String} representing the name of a workspace.
     * @param binaryAbsPath a non-null {@link String} representing the absolute path to a binary property.
     * @return the {@link Property} instance which is located at the given path. If such a property is not located, an exception
     *         is thrown.
     * @throws RepositoryException if any JCR related operation fails, including the case when the path to the property isn't valid.
     */
    public Property getBinaryProperty( HttpServletRequest request,
                                       String repositoryName,
                                       String workspaceName,
                                       String binaryAbsPath ) throws RepositoryException {
        Session session = getSession(request, repositoryName, workspaceName);
        return session.getProperty(binaryAbsPath);
    }

    /**
     * Returns a default Content-Disposition {@link String} for a given binary property.
     *
     * @param binaryProperty a non-null {@link Property}
     * @return a non-null String which represents a valid Content-Disposition.
     * @throws RepositoryException if any JCR related operation involving the binary property fail.
     */
    public String getDefaultContentDisposition( Property binaryProperty ) throws RepositoryException {
        Node parentNode = getParentNode(binaryProperty);
        String parentName = parentNode.getName();
        if (StringUtil.isBlank(parentName)) {
            parentName = "binary";
        }
        return DEFAULT_CONTENT_DISPOSITION_PREFIX + parentName;
    }

    /**
     * Returns the default mime-type of a given binary property.
     *
     * @param binaryProperty a non-null {@link Property}
     * @return a non-null String which represents the mime-type of the binary property.
     * @throws RepositoryException if any JCR related operation involving the binary property fail.
     */
    public String getDefaultMimeType( Property binaryProperty ) throws RepositoryException {
        try {
            Binary binary = binaryProperty.getBinary();
            return binary instanceof org.modeshape.jcr.api.Binary ? ((org.modeshape.jcr.api.Binary)binary)
                    .getMimeType() : DEFAULT_MIME_TYPE;
        } catch (IOException e) {
            logger.warn("Cannot determine default mime-type", e);
            return DEFAULT_MIME_TYPE;
        }
    }

    /**
     * Updates the {@link Property property} at the given path with the content from the given {@link InputStream}.
     *
     * @param request a non-null {@link HttpServletRequest} request
     * @param repositoryName a non-null {@link String} representing the name of a repository.
     * @param workspaceName a non-null {@link String} representing the name of a workspace.
     * @param binaryPropertyAbsPath a non-null {@link String} representing the absolute path to a binary property.
     * @param binaryStream an {@link InputStream} which represents the new content of the binary property.
     * @param allowCreation a boolean flag which indicates what the behavior should be in case such a property does
     * not exist on its parent node: if the flag is {@code true}, the property will be created, otherwise a response code indicating
     * the absence is returned.
     * @return a {@link Response} object, which is either OK and contains the rest representation of the binary property, or is
     *         NOT_FOUND.
     * @throws RepositoryException if any JCR related operations fail
     * @throws IllegalArgumentException if the given input stream is {@code null}
     */
    public Response updateBinary( HttpServletRequest request,
                                  String repositoryName,
                                  String workspaceName,
                                  String binaryPropertyAbsPath,
                                  InputStream binaryStream,
                                  boolean allowCreation ) throws RepositoryException {
        //TODO author=Horia Chiorean date=8/9/12 description=We don't have a way (without changing the API) to set the mime-type on a binary
        CheckArg.isNotNull(binaryStream, "request body");

        String parentPath = parentPath(binaryPropertyAbsPath);
        Session session = getSession(request, repositoryName, workspaceName);
        Node parent = (Node)itemAtPath(parentPath, session);

        int lastSlashInd = binaryPropertyAbsPath.lastIndexOf('/');
        String propertyName = lastSlashInd == -1 ? binaryPropertyAbsPath : binaryPropertyAbsPath.substring(lastSlashInd + 1);
        boolean createdNewValue = false;
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
                createdNewValue = true;
            }
            session.save();
            RestProperty restItem = (RestProperty)createRestItem(request, 0, session, binaryProperty);
            return createdNewValue ? Response.status(Response.Status.CREATED).entity(restItem).build() :
                    Response.ok().entity(restItem).build();
        } finally {
            try {
                binaryStream.close();
            } catch (IOException e) {
                logger.error("Cannot close binary stream", e);
            }
        }
    }
}
