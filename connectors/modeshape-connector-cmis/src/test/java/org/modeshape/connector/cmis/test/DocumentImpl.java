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

import java.io.IOException;
import java.util.*;
import org.apache.chemistry.opencmis.client.api.*;
import org.apache.chemistry.opencmis.commons.data.*;
import org.apache.chemistry.opencmis.commons.enums.PropertyType;
import org.apache.chemistry.opencmis.commons.enums.VersioningState;

/**
 *
 * @author kulikov
 */
public class DocumentImpl extends CmisObjectImpl implements Document {

    private Folder parent;
    private CmisRepository repository;
    private BinaryValue binaryValue;

    public DocumentImpl(CmisRepository repository, Folder parent, Map<String, ?> params) {
        super(params);
        this.parent = parent;
        this.repository = repository;
        properties.add(new PropertyImpl(PropertyType.STRING,
                "cmis:baseTypeId", "Base Type Id", "cmis:baseTypeId", "cmis:baseTypeId", "cmis:document"));
        properties.add(new PropertyImpl(PropertyType.STRING,
                "cmis:objectTypeId", "Object Type Id", "cmis:objectTypeId", "cmis:objectTypeId", "cmis:document"));
    }

    @Override
    public TransientDocument getTransientDocument() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void deleteAllVersions() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public ContentStream getContentStream() {
        return binaryValue;
    }

    @Override
    public ContentStream getContentStream(String string) {
        return binaryValue;
    }

    @Override
    public Document setContentStream(ContentStream binaryValue, boolean bln) {
        try {
            this.binaryValue = new BinaryValue(binaryValue);
        } catch (IOException e) {
        }
        return this;
    }

    @Override
    public ObjectId setContentStream(ContentStream stream, boolean bln, boolean bln1) {
        try {
            this.binaryValue = new BinaryValue(stream);
        } catch (IOException e) {
        }
        return new ObjectIdImpl(this.getId());
    }

    @Override
    public Document deleteContentStream() {
        this.binaryValue = null;
        return this;
    }

    @Override
    public ObjectId deleteContentStream(boolean bln) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public ObjectId checkOut() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void cancelCheckOut() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public ObjectId checkIn(boolean bln, Map<String, ?> map, ContentStream stream, String string, List<Policy> list, List<Ace> list1, List<Ace> list2) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public ObjectId checkIn(boolean bln, Map<String, ?> map, ContentStream stream, String string) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Document getObjectOfLatestVersion(boolean bln) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Document getObjectOfLatestVersion(boolean bln, OperationContext oc) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<Document> getAllVersions() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<Document> getAllVersions(OperationContext oc) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Document copy(ObjectId oi) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Document copy(ObjectId oi, Map<String, ?> map, VersioningState vs, List<Policy> list, List<Ace> list1, List<Ace> list2, OperationContext oc) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public FileableCmisObject move(ObjectId oi, ObjectId oi1) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public FileableCmisObject move(ObjectId oi, ObjectId oi1, OperationContext oc) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<Folder> getParents() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<Folder> getParents(OperationContext oc) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<String> getPaths() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void addToFolder(ObjectId oi, boolean bln) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void removeFromFolder(ObjectId oi) {
        throw new UnsupportedOperationException("Not supported yet.");
    }


    @Override
    public Boolean isImmutable() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Boolean isLatestVersion() {
        return true;
    }

    @Override
    public Boolean isMajorVersion() {
        return true;
    }

    @Override
    public Boolean isLatestMajorVersion() {
        return true;
    }

    @Override
    public String getVersionLabel() {
        return "1.1";
    }

    @Override
    public String getVersionSeriesId() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Boolean isVersionSeriesCheckedOut() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getVersionSeriesCheckedOutBy() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getVersionSeriesCheckedOutId() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getCheckinComment() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public long getContentStreamLength() {
        return binaryValue.getLength();
    }

    @Override
    public String getContentStreamMimeType() {
        return binaryValue.getMimeType();
    }

    @Override
    public String getContentStreamFileName() {
        return binaryValue.getFileName();
    }

    @Override
    public String getContentStreamId() {
        return this.getId();
    }

}
