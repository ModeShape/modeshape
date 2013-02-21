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
package org.modeshape.connector.cmis.test;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import org.apache.chemistry.opencmis.client.api.*;
import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.data.Ace;
import org.apache.chemistry.opencmis.commons.data.Acl;
import org.apache.chemistry.opencmis.commons.data.AllowableActions;
import org.apache.chemistry.opencmis.commons.data.CmisExtensionElement;
import org.apache.chemistry.opencmis.commons.enums.AclPropagation;
import org.apache.chemistry.opencmis.commons.enums.BaseTypeId;
import org.apache.chemistry.opencmis.commons.enums.ExtensionLevel;
import org.apache.chemistry.opencmis.commons.enums.PropertyType;

/**
 *
 * @author kulikov
 */
public class CmisObjectImpl implements CmisObject {

    protected ArrayList<Property<?>> properties = new ArrayList();
    private MessageDigest md;

    public CmisObjectImpl(Map<String, ?> params) {
        try {
            md = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
        }

        String path = (String) params.get(PropertyIds.PATH);
        String name = (String) params.get(PropertyIds.NAME);

        String fqn = path + "/" + name;
        String id = new String(md.digest(fqn.getBytes()));

        String baseTypeId = (String) params.get(PropertyIds.BASE_TYPE_ID);
        String objectTypeId = (String) params.get(PropertyIds.OBJECT_TYPE_ID);
        String createdBy = (String) params.get(PropertyIds.CREATED_BY);

        GregorianCalendar now = new GregorianCalendar();

        properties.add(new PropertyImpl(PropertyType.ID,
                "cmis:objectId", "Object Id", "cmis:objectId", "cmis:objectId", id));
        properties.add(new PropertyImpl(PropertyType.STRING,
                "cmis:path", "Path", "cmis:path", "cmis:path", path));
        properties.add(new PropertyImpl(PropertyType.STRING,
                "cmis:name", "Name", "cmis:name", "cmis:name", name));
//        properties.add(new PropertyImpl(PropertyType.STRING,
//                "cmis:baseTypeId", "Base Type Id", "cmis:baseTypeId", "cmis:baseTypeId", baseTypeId));
//        properties.add(new PropertyImpl(PropertyType.STRING,
//                "cmis:objectTypeId", "Object Type Id", "cmis:objectTypeId", "cmis:objectTypeId", objectTypeId));
        properties.add(new PropertyImpl(PropertyType.STRING,
                "cmis:createdBy", "Created By", "cmis:createdBy", "cmis:createdBy", createdBy));
        properties.add(new PropertyImpl(PropertyType.STRING,
                "cmis:lastModifiedBy", "Last Modified By", "cmis:lastModifiedBy", "cmis:lastModifiedBy", createdBy));
        properties.add(new PropertyImpl(PropertyType.DATETIME,
                "cmis:creationDate", "Creation Date", "cmis:creationDate", "cmis:creationDate", now));
        properties.add(new PropertyImpl(PropertyType.DATETIME,
                "cmis:lastModificationDate", "Last Modification Date", "cmis:lastModificationDate", "cmis:lastModificationDate", now));
    }

    @Override
    public AllowableActions getAllowableActions() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<Relationship> getRelationships() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Acl getAcl() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void delete(boolean bln) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public CmisObject updateProperties(Map<String, ?> map) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public ObjectId updateProperties(Map<String, ?> map, boolean bln) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<Rendition> getRenditions() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void applyPolicy(ObjectId... ois) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void removePolicy(ObjectId... ois) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<Policy> getPolicies() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Acl applyAcl(List<Ace> list, List<Ace> list1, AclPropagation ap) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Acl addAcl(List<Ace> list, AclPropagation ap) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Acl removeAcl(List<Ace> list, AclPropagation ap) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<CmisExtensionElement> getExtensions(ExtensionLevel el) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public <T> T getAdapter(Class<T> type) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public TransientCmisObject getTransientObject() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public long getRefreshTimestamp() {
        return System.currentTimeMillis();
    }

    @Override
    public void refresh() {
    }

    @Override
    public void refreshIfOld(long l) {
    }

    @Override
    public String getId() {
        return this.getPropertyValue("cmis:objectId");
    }

    @Override
    public List<Property<?>> getProperties() {
        return properties;
    }

    @Override
    public <T> Property<T> getProperty(String id) {
        for (Property property : properties) {
            if (property.getId().equals(id)) {
                return property;
            }
        }
        return null;
    }

    @Override
    public <T> T getPropertyValue(String id) {
        Property p = getProperty(id);
        return p != null ? (T)p.getValue() : null;
    }

    @Override
    public String getName() {
        return this.getPropertyValue("cmis:name");
    }

    @Override
    public String getCreatedBy() {
        return this.getPropertyValue("cmis:createdBy");
    }

    @Override
    public GregorianCalendar getCreationDate() {
        return this.getPropertyValue("cmis:creationDate");
    }

    @Override
    public String getLastModifiedBy() {
        return this.getPropertyValue("cmis:lastModifiedBy");
    }

    @Override
    public GregorianCalendar getLastModificationDate() {
        return this.getPropertyValue("cmis:lastModificationDate");
    }

    @Override
    public BaseTypeId getBaseTypeId() {
        return BaseTypeId.fromValue((String)getPropertyValue("cmis:baseTypeId"));
    }

    @Override
    public ObjectType getBaseType() {
        return new ObjectTypeImpl((String)getPropertyValue("cmis:lastModifiedBy"));
    }

    @Override
    public ObjectType getType() {
        return new ObjectTypeImpl((String)getPropertyValue("cmis:objectTypeId"));
    }

    @Override
    public String getChangeToken() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    protected String path() {
        return this.getPropertyValue("cmis:path");
    }

}
