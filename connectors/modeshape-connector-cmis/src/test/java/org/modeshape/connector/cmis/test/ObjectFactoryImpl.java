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

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.chemistry.opencmis.client.api.*;
import org.apache.chemistry.opencmis.commons.data.*;
import org.apache.chemistry.opencmis.commons.definitions.PropertyDefinition;
import org.apache.chemistry.opencmis.commons.definitions.TypeDefinition;
import org.apache.chemistry.opencmis.commons.enums.Updatability;

/**
 *
 * @author kulikov
 */
public class ObjectFactoryImpl implements ObjectFactory {

    @Override
    public void initialize(Session sn, Map<String, String> map) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public RepositoryInfo convertRepositoryInfo(RepositoryInfo ri) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Acl convertAces(List<Ace> list) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Acl createAcl(List<Ace> list) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Ace createAce(String string, List<String> list) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<String> convertPolicies(List<Policy> list) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Rendition convertRendition(String string, RenditionData rd) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public ContentStream createContentStream(String string, long l, String string1, InputStream in) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public ContentStream convertContentStream(ContentStream stream) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public ObjectType convertTypeDefinition(TypeDefinition td) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public ObjectType getTypeFromObjectData(ObjectData od) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public <T> Property<T> createProperty(PropertyDefinition<T> pd, List<T> list) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Map<String, Property<?>> convertProperties(ObjectType ot, Properties prprts) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Properties convertProperties(Map<String, ?> map, ObjectType ot, Set<Updatability> set) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<PropertyData<?>> convertQueryProperties(Properties prprts) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public CmisObject convertObject(ObjectData od, OperationContext oc) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public QueryResult convertQueryResult(ObjectData od) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public ChangeEvent convertChangeEvent(ObjectData od) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public ChangeEvents convertChangeEvents(String string, ObjectList ol) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
