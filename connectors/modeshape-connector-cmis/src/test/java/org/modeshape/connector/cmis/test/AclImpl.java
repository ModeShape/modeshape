/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.modeshape.connector.cmis.test;

import java.util.List;
import org.apache.chemistry.opencmis.commons.data.Ace;
import org.apache.chemistry.opencmis.commons.data.Acl;
import org.apache.chemistry.opencmis.commons.data.CmisExtensionElement;

/**
 *
 * @author kulikov
 */
public class AclImpl implements Acl {

    @Override
    public List<Ace> getAces() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Boolean isExact() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<CmisExtensionElement> getExtensions() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setExtensions(List<CmisExtensionElement> list) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
