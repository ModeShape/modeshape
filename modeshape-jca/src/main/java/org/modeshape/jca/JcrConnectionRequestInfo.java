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
package org.modeshape.jca;

import java.util.HashMap;
import java.util.Map;
import javax.jcr.Credentials;
import javax.jcr.SimpleCredentials;
import javax.resource.spi.ConnectionRequestInfo;
import org.modeshape.common.annotation.Immutable;

/**
 * Provides implementation for the connection request to the Modeshape
 * repository via JCA connector.
 *
 * @author kulikov
 */
@Immutable
public class JcrConnectionRequestInfo implements ConnectionRequestInfo {
    /**
     * Credentials.
     */
    private final Credentials creds;

    /**
     * Workspace.
     */
    private final String workspace;

    //Hashcode
    private final int hash;

    /**
     * Construct the request info.
     *
     * @param  creds user's credentials
     * @param workspace Repository workspace name.
     */
    public JcrConnectionRequestInfo(Credentials creds, String workspace) {
        this.creds = creds;
        this.workspace = workspace;
        this.hash = computeHash();
    }

    /**
     * Gets the workspace.
     *
     * @return workspace name
     */
    public String getWorkspace() {
        return workspace;
    }

    /**
     * Gets the credentials.
     *
     * @return Credential object
     */
    public Credentials getCredentials() {
        return creds;
    }

    @Override
    public int hashCode() {
        return hash;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o instanceof JcrConnectionRequestInfo) {
            return equals((JcrConnectionRequestInfo) o);
        } else {
            return false;
        }
    }

    /**
     * Compares specified request with this one.
     *
     * @param o the request object to compare.
     * @return true if specified object equals to this one.
     */
    private boolean equals(JcrConnectionRequestInfo o) {
        return equals(workspace, o.workspace)
            && equals(creds, o.creds);
    }

    /**
     * Compares two objects.
     *
     * @param o1 first object
     * @param o2 second object.
     * @return true if both objects are not null and both are equals in
     * sense of object.equal(..) method.
     */
    private boolean equals(Object o1, Object o2) {
        if (o1 == o2) {
            return true;
        } else if ((o1 != null) && (o2 != null)) {
            return o1.equals(o2);
        } else {
            return false;
        }
    }

    /**
     * Compares two character sequences.
     *
     * @param o1 first character sequence
     * @param o2 second character sequence.
     * @return true if both sequences has same length and same respective characters
     */
    private boolean equals(char[] o1, char[] o2) {
        if (o1 == o2) {
            return true;
        } else if ((o1 != null) && (o2 != null)) {
            return equals(new String(o1), new String(o2));
        } else {
            return false;
        }
    }

    /**
     * Compares to arguments
     *
     * @param o1 argument 1
     * @param o2 argument 2
     * @return true if both arguments are equal.
     */
    private boolean equals(Credentials o1, Credentials o2) {
        if (o1 == o2) {
            return true;
        } else if ((o1 != null) && (o2 != null)) {
            if ((o1 instanceof SimpleCredentials) && (o2 instanceof SimpleCredentials)) {
                return equals((SimpleCredentials) o1, (SimpleCredentials) o2);
            } else {
                return o1.equals(o2);
            }
        } else {
            return false;
        }
    }

    /**
     * This method compares two simple credentials.
     */
    private boolean equals(SimpleCredentials o1, SimpleCredentials o2) {
        if (!equals(o1.getUserID(), o2.getUserID())) {
            return false;
        }

        if (!equals(o1.getPassword(), o2.getPassword())) {
            return false;
        }

        Map m1 = getAttributeMap(o1);
        Map m2 = getAttributeMap(o2);
        return m1.equals(m2);
    }

    /**
     * Return the credentials attributes.
     */
    private Map getAttributeMap(SimpleCredentials creds) {
        HashMap map = new HashMap();
        String[] keys = creds.getAttributeNames();

        for (int i = 0; i < keys.length; i++) {
            map.put(keys[i], creds.getAttribute(keys[i]));
        }

        return map;
    }

    /**
     * Returns Credentials instance hash code. Handles instances of
     * SimpleCredentials in a special way.
     */
    private int computeCredsHashCode(Credentials c) {
        if (c instanceof SimpleCredentials) {
            return computeSimpleCredsHashCode((SimpleCredentials) c);
        }
        return c.hashCode();
    }

    /**
     * Computes hash code of a SimpleCredentials instance. Ignores its own
     * hashCode() method because it's not overridden in SimpleCredentials.
     */
    private int computeSimpleCredsHashCode(SimpleCredentials c) {
        String userID = c.getUserID();
        char[] password = c.getPassword();
        Map m = getAttributeMap(c);
        final int prime = 31;
        int result = 1;
        result = prime * result + ((userID == null) ? 0 : userID.hashCode());
        for (int i = 0; i < password.length; i++) {
            result = prime * result + password[i];
        }
        result = prime * result + ((m == null) ? 0 : m.hashCode());
        return result;
    }

    private int computeHash() {
        int hash1 = workspace != null ? workspace.hashCode() : 0;
        int hash2 = creds != null ? computeCredsHashCode(creds) : 0;
        return hash1 ^ hash2;
    }
}
