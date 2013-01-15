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
package org.modeshape.cmis;

import org.apache.chemistry.opencmis.commons.data.Acl;
import org.apache.chemistry.opencmis.commons.data.AllowableActions;
import org.apache.chemistry.opencmis.commons.data.ContentStream;
import org.apache.chemistry.opencmis.commons.data.ExtensionsData;
import org.apache.chemistry.opencmis.commons.data.FailedToDeleteData;
import org.apache.chemistry.opencmis.commons.data.ObjectData;
import org.apache.chemistry.opencmis.commons.data.ObjectInFolderContainer;
import org.apache.chemistry.opencmis.commons.data.ObjectInFolderList;
import org.apache.chemistry.opencmis.commons.data.ObjectList;
import org.apache.chemistry.opencmis.commons.data.ObjectParentData;
import org.apache.chemistry.opencmis.commons.data.Properties;
import org.apache.chemistry.opencmis.commons.data.RepositoryInfo;
import org.apache.chemistry.opencmis.commons.definitions.TypeDefinition;
import org.apache.chemistry.opencmis.commons.definitions.TypeDefinitionContainer;
import org.apache.chemistry.opencmis.commons.definitions.TypeDefinitionList;
import org.apache.chemistry.opencmis.commons.enums.IncludeRelationships;
import org.apache.chemistry.opencmis.commons.enums.UnfileObject;
import org.apache.chemistry.opencmis.commons.enums.VersioningState;
import org.apache.chemistry.opencmis.commons.exceptions.CmisRuntimeException;
import org.apache.chemistry.opencmis.commons.impl.server.AbstractCmisService;
import org.apache.chemistry.opencmis.commons.server.CallContext;
import org.apache.chemistry.opencmis.commons.spi.Holder;

import javax.jcr.Credentials;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import java.math.BigInteger;
import java.util.*;
import org.apache.chemistry.opencmis.jcr.JcrRepository;

/**
 * JCR service implementation.
 *
 * @author kulikov
 */
public class JcrService extends AbstractCmisService {
    private final Map<String,JcrRepository> jcrRepositories;
    private final Map<String, Session> sessions = new HashMap<String, Session>();

    private CallContext context;

    public JcrService(Map<String,JcrRepository> jcrRepositories) {
        this.jcrRepositories = jcrRepositories;
    }

    @Override
    public void close() {
        for (Session session : sessions.values()) {
            session.logout();
        }

        super.close();
    }

    public void setCallContext(CallContext context) {
        this.context = context;
    }

    public CallContext getCallContext() {
        return context;
    }

    //------------------------------------------< repository service >---

    @Override
    public RepositoryInfo getRepositoryInfo(String repositoryId, ExtensionsData extension) {
        System.out.println("-- getting repository info");
        RepositoryInfo info = jcrRepository(repositoryId).getRepositoryInfo(login(repositoryId));
        return new RepositoryInfoImpl(repositoryId, info);
    }

    @Override
    public List<RepositoryInfo> getRepositoryInfos(ExtensionsData extension) {
        ArrayList<RepositoryInfo> info = new ArrayList();
        Set<String> IDs = jcrRepositories.keySet();
        for (String Id : IDs) {
            JcrRepository repo = jcrRepositories.get(Id);
            info.addAll(repo.getRepositoryInfos(login(name(Id))));
        }
        return info;
    }

    @Override
    public TypeDefinitionList getTypeChildren(String repositoryId, String typeId, Boolean includePropertyDefinitions,
            BigInteger maxItems, BigInteger skipCount, ExtensionsData extension) {

        return jcrRepository(repositoryId).getTypeChildren(login(repositoryId), typeId, includePropertyDefinitions, maxItems, skipCount);
    }

    @Override
    public TypeDefinition getTypeDefinition(String repositoryId, String typeId, ExtensionsData extension) {
        return jcrRepository(repositoryId).getTypeDefinition(login(repositoryId), typeId);
    }

    @Override
    public List<TypeDefinitionContainer> getTypeDescendants(String repositoryId, String typeId, BigInteger depth,
            Boolean includePropertyDefinitions, ExtensionsData extension) {

        return jcrRepository(repositoryId).getTypesDescendants(login(repositoryId), typeId, depth, includePropertyDefinitions);
    }

    //------------------------------------------< navigation service >---

    @Override
    public ObjectInFolderList getChildren(String repositoryId, String folderId, String filter, String orderBy,
            Boolean includeAllowableActions, IncludeRelationships includeRelationships, String renditionFilter,
            Boolean includePathSegment, BigInteger maxItems, BigInteger skipCount, ExtensionsData extension) {

        return jcrRepository(repositoryId).getChildren(login(repositoryId), folderId, filter, includeAllowableActions,
                includePathSegment, maxItems, skipCount, this, context.isObjectInfoRequired());
    }

    @Override
    public List<ObjectInFolderContainer> getDescendants(String repositoryId, String folderId, BigInteger depth,
            String filter, Boolean includeAllowableActions, IncludeRelationships includeRelationships,
            String renditionFilter, Boolean includePathSegment, ExtensionsData extension) {

        return jcrRepository(repositoryId).getDescendants(login(repositoryId), folderId, depth, filter, includeAllowableActions,
                includePathSegment, this, context.isObjectInfoRequired(), false);
    }

    @Override
    public ObjectData getFolderParent(String repositoryId, String folderId, String filter, ExtensionsData extension) {
        return jcrRepository(repositoryId).getFolderParent(login(repositoryId), folderId, filter, this, context.isObjectInfoRequired());
    }

    @Override
    public List<ObjectInFolderContainer> getFolderTree(String repositoryId, String folderId, BigInteger depth,
            String filter, Boolean includeAllowableActions, IncludeRelationships includeRelationships,
            String renditionFilter, Boolean includePathSegment, ExtensionsData extension) {

        return jcrRepository(repositoryId).getDescendants(login(repositoryId), folderId, depth, filter, includeAllowableActions,
                includePathSegment, this, context.isObjectInfoRequired(), true);
    }

    @Override
    public List<ObjectParentData> getObjectParents(String repositoryId, String objectId, String filter,
            Boolean includeAllowableActions, IncludeRelationships includeRelationships, String renditionFilter,
            Boolean includeRelativePathSegment, ExtensionsData extension) {

        return jcrRepository(repositoryId).getObjectParents(login(repositoryId), objectId, filter, includeAllowableActions,
                includeRelativePathSegment, this, context.isObjectInfoRequired());
    }

    @Override
    public ObjectList getCheckedOutDocs(String repositoryId, String folderId, String filter, String orderBy,
            Boolean includeAllowableActions, IncludeRelationships includeRelationships, String renditionFilter,
            BigInteger maxItems, BigInteger skipCount, ExtensionsData extension) {

        return jcrRepository(repositoryId).getCheckedOutDocs(login(repositoryId), folderId, filter, orderBy, includeAllowableActions,
                maxItems, skipCount);
    }

    //------------------------------------------< object service >---

    @Override
    public String createDocument(String repositoryId, Properties properties, String folderId,
            ContentStream contentStream, VersioningState versioningState, List<String> policies, Acl addAces,
            Acl removeAces, ExtensionsData extension) {

        return jcrRepository(repositoryId).createDocument(login(repositoryId), properties, folderId, contentStream, versioningState);
    }

    @Override
    public String createDocumentFromSource(String repositoryId, String sourceId, Properties properties,
            String folderId, VersioningState versioningState, List<String> policies, Acl addAces, Acl removeAces,
            ExtensionsData extension) {

        return jcrRepository(repositoryId).createDocumentFromSource(login(repositoryId), sourceId, properties, folderId, versioningState);
    }

    @Override
    public void setContentStream(String repositoryId, Holder<String> objectId, Boolean overwriteFlag,
            Holder<String> changeToken, ContentStream contentStream, ExtensionsData extension) {

        jcrRepository(repositoryId).setContentStream(login(repositoryId), objectId, overwriteFlag, contentStream);
    }

    @Override
    public void deleteContentStream(String repositoryId, Holder<String> objectId, Holder<String> changeToken,
            ExtensionsData extension) {

        jcrRepository(repositoryId).setContentStream(login(repositoryId), objectId, true, null);
    }

    @Override
    public String createFolder(String repositoryId, Properties properties, String folderId, List<String> policies,
            Acl addAces, Acl removeAces, ExtensionsData extension) {

        return jcrRepository(repositoryId).createFolder(login(repositoryId), properties, folderId);
    }

    @Override
    public void deleteObjectOrCancelCheckOut(String repositoryId, String objectId, Boolean allVersions,
            ExtensionsData extension) {

        jcrRepository(repositoryId).deleteObject(login(repositoryId), objectId, allVersions);
    }

    @Override
    public FailedToDeleteData deleteTree(String repositoryId, String folderId, Boolean allVersions,
            UnfileObject unfileObjects, Boolean continueOnFailure, ExtensionsData extension) {

        return jcrRepository(repositoryId).deleteTree(login(repositoryId), folderId);
    }

    @Override
    public AllowableActions getAllowableActions(String repositoryId, String objectId, ExtensionsData extension) {
        return jcrRepository(repositoryId).getAllowableActions(login(repositoryId), objectId);
    }

    @Override
    public ContentStream getContentStream(String repositoryId, String objectId, String streamId, BigInteger offset,
            BigInteger length, ExtensionsData extension) {

        return jcrRepository(repositoryId).getContentStream(login(repositoryId), objectId, offset, length);
    }

    @Override
    public ObjectData getObject(String repositoryId, String objectId, String filter, Boolean includeAllowableActions,
            IncludeRelationships includeRelationships, String renditionFilter, Boolean includePolicyIds,
            Boolean includeAcl, ExtensionsData extension) {

        return jcrRepository(repositoryId).getObject(login(repositoryId), objectId, filter, includeAllowableActions, this,
                context.isObjectInfoRequired());
    }

    @Override
    public ObjectData getObjectByPath(String repositoryId, String path, String filter, Boolean includeAllowableActions,
            IncludeRelationships includeRelationships, String renditionFilter, Boolean includePolicyIds,
            Boolean includeAcl, ExtensionsData extension) {

        return jcrRepository(repositoryId).getObjectByPath(login(repositoryId), path, filter, includeAllowableActions, includeAcl,
                this, context.isObjectInfoRequired());
    }

    @Override
    public Properties getProperties(String repositoryId, String objectId, String filter, ExtensionsData extension) {
        return jcrRepository(repositoryId).getProperties(login(repositoryId), objectId, filter, false, this,
                context.isObjectInfoRequired());
    }

    @Override
    public void moveObject(String repositoryId, Holder<String> objectId, String targetFolderId, String sourceFolderId,
            ExtensionsData extension) {

        jcrRepository(repositoryId).moveObject(login(repositoryId), objectId, targetFolderId, this, context.isObjectInfoRequired());
    }

    @Override
    public void updateProperties(String repositoryId, Holder<String> objectId, Holder<String> changeToken,
            Properties properties, ExtensionsData extension) {

        jcrRepository(repositoryId).updateProperties(login(repositoryId), objectId, properties, this, context.isObjectInfoRequired());
    }

    //------------------------------------------< versioning service >---

    @Override
    public void checkOut(String repositoryId, Holder<String> objectId, ExtensionsData extension,
            Holder<Boolean> contentCopied) {

        jcrRepository(repositoryId).checkOut(login(repositoryId), objectId, contentCopied);
    }

    @Override
    public void cancelCheckOut(String repositoryId, String objectId, ExtensionsData extension) {
        jcrRepository(repositoryId).cancelCheckout(login(repositoryId), objectId);
    }

    @Override
    public void checkIn(String repositoryId, Holder<String> objectId, Boolean major, Properties properties,
            ContentStream contentStream, String checkinComment, List<String> policies, Acl addAces, Acl removeAces,
            ExtensionsData extension) {

        jcrRepository(repositoryId).checkIn(login(repositoryId), objectId, major, properties, contentStream, checkinComment);
    }

    @Override
    public List<ObjectData> getAllVersions(String repositoryId, String objectId, String versionSeriesId, String filter,
            Boolean includeAllowableActions, ExtensionsData extension) {

        return jcrRepository(repositoryId).getAllVersions(login(repositoryId), versionSeriesId == null ? objectId : versionSeriesId,
                filter, includeAllowableActions, this, context.isObjectInfoRequired());
    }

    @Override
    public ObjectData getObjectOfLatestVersion(String repositoryId, String objectId, String versionSeriesId,
            Boolean major, String filter, Boolean includeAllowableActions, IncludeRelationships includeRelationships,
            String renditionFilter, Boolean includePolicyIds, Boolean includeAcl, ExtensionsData extension) {

        return jcrRepository(repositoryId).getObject(login(repositoryId), versionSeriesId == null ? objectId : versionSeriesId,
                filter, includeAllowableActions, this, context.isObjectInfoRequired());
    }

    @Override
    public Properties getPropertiesOfLatestVersion(String repositoryId, String objectId, String versionSeriesId,
            Boolean major, String filter, ExtensionsData extension) {

        ObjectData object = getObjectOfLatestVersion(repositoryId, objectId, versionSeriesId, major, filter, false,
                null, null, false, false, extension);

        return object.getProperties();
    }

    // --- discovery service ---

    @Override
    public ObjectList query(String repositoryId, String statement, Boolean searchAllVersions,
            Boolean includeAllowableActions, IncludeRelationships includeRelationships, String renditionFilter,
            BigInteger maxItems, BigInteger skipCount, ExtensionsData extension) {

        return jcrRepository(repositoryId).query(login(repositoryId), statement, searchAllVersions, includeAllowableActions,
                maxItems, skipCount);
    }

    //------------------------------------------< protected >---

    protected Session login(String repositoryId) {
        System.out.println("--- login: " + repositoryId);

        if (context == null) {
            throw new CmisRuntimeException("No user context!");
        }

        Session session = sessions.get(repositoryId);
        if (session == null) {
            String userName = context.getUsername();
            String password = context.getPassword();
            Credentials credentials = userName == null
                ? null
                : new SimpleCredentials(userName, password == null ? "".toCharArray() : password.toCharArray());

            try {
                session = jcrRepository(repositoryId).login(credentials, workspace(repositoryId));
                sessions.put(repositoryId, session);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return session;
    }

    private JcrRepository jcrRepository(String repositoryId) {
        return jcrRepositories.get(name(repositoryId));
    }

    private String name(String repositoryId) {
        return repositoryId.indexOf(":") > 0 ? repositoryId.substring(0, repositoryId.indexOf(":")) : repositoryId;
    }

    private String workspace(String repositoryId) {
        return repositoryId.indexOf(":") > 0 ? null : repositoryId.substring(repositoryId.indexOf(":"), repositoryId.length());
    }

}
