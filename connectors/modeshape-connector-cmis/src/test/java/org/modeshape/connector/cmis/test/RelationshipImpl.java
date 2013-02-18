/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.modeshape.connector.cmis.test;

import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import org.apache.chemistry.opencmis.client.api.*;
import org.apache.chemistry.opencmis.commons.data.Ace;
import org.apache.chemistry.opencmis.commons.data.Acl;
import org.apache.chemistry.opencmis.commons.data.AllowableActions;
import org.apache.chemistry.opencmis.commons.data.CmisExtensionElement;
import org.apache.chemistry.opencmis.commons.enums.AclPropagation;
import org.apache.chemistry.opencmis.commons.enums.BaseTypeId;
import org.apache.chemistry.opencmis.commons.enums.ExtensionLevel;

/**
 *
 * @author kulikov
 */
public class RelationshipImpl implements Relationship {

    @Override
    public TransientRelationship getTransientRelationship() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public CmisObject getSource() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public CmisObject getSource(OperationContext oc) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public CmisObject getTarget() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public CmisObject getTarget(OperationContext oc) {
        throw new UnsupportedOperationException("Not supported yet.");
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
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void refresh() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void refreshIfOld(long l) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getId() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<Property<?>> getProperties() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public <T> Property<T> getProperty(String string) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public <T> T getPropertyValue(String string) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getName() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getCreatedBy() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public GregorianCalendar getCreationDate() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getLastModifiedBy() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public GregorianCalendar getLastModificationDate() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public BaseTypeId getBaseTypeId() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public ObjectType getBaseType() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public ObjectType getType() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getChangeToken() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public ObjectId getSourceId() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public ObjectId getTargetId() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
