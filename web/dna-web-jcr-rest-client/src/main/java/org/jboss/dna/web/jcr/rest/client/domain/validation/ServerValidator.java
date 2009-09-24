/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * JBoss DNA is free software. Unless otherwise indicated, all code in JBoss DNA
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * JBoss DNA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.web.jcr.rest.client.domain.validation;

import java.net.URL;
import org.jboss.dna.web.jcr.rest.client.RestClientI18n;
import org.jboss.dna.web.jcr.rest.client.ServerManager;
import org.jboss.dna.web.jcr.rest.client.Status;
import org.jboss.dna.web.jcr.rest.client.Status.Severity;
import org.jboss.dna.web.jcr.rest.client.domain.Server;

/**
 * The <code>ServerValidator</code> class provides validation for creating a {@link Server DNA server}.
 */
public final class ServerValidator {

    // ===========================================================================================================================
    // Class Methods
    // ===========================================================================================================================

    /**
     * @param password the password being validated
     * @return a validation status (never <code>null</code>)
     */
    public static Status isPasswordValid( String password ) {
        return Status.OK_STATUS;
    }

    /**
     * @param url the URL being validated
     * @return a validation status (never <code>null</code>)
     */
    public static Status isUrlValid( String url ) {
        if ((url == null) || (url.length() == 0)) {
            return new Status(Severity.ERROR, RestClientI18n.serverEmptyUrlMsg.text(), null);
        }

        try {
            new URL(url);
        } catch (Exception e) {
            return new Status(Severity.ERROR, RestClientI18n.serverInvalidUrlMsg.text(url), null);
        }

        return Status.OK_STATUS;
    }

    /**
     * @param user the user being validated
     * @return a validation status (never <code>null</code>)
     */
    public static Status isUserValid( String user ) {
        if ((user == null) || (user.length() == 0)) {
            return new Status(Severity.ERROR, RestClientI18n.serverEmptyUserMsg.text(), null);
        }

        return Status.OK_STATUS;
    }

    /**
     * This does not verify that a server with the same primary field values doesn't already exist.
     * 
     * @param url the URL being validated
     * @param user the user being validated
     * @param password the password being validated
     * @param persistPassword <code>true</code> if the password should be persisted
     * @return a validation status (never <code>null</code>)
     */
    public static Status isValid( String url,
                                  String user,
                                  String password,
                                  boolean persistPassword ) {
        Status status = isUrlValid(url);

        if (!status.isError()) {
            status = isUserValid(user);

            if (!status.isError()) {
                status = isPasswordValid(password);
            }
        }

        return status;
    }

    /**
     * Validates the server properties and makes sure no other exists in the server registry that also has the same primary field
     * values.
     * 
     * @param url the URL being validated
     * @param user the user being validated
     * @param password the password being validated
     * @param persistPassword <code>true</code> if the password should be persisted
     * @param serverManager the server manager controlling the server registry (may not be <code>null</code>)
     * @return a validation status (never <code>null</code>)
     * @see #isValid(String, String, String, boolean)
     */
    public static Status isValid( String url,
                                  String user,
                                  String password,
                                  boolean persistPassword,
                                  ServerManager serverManager ) {
        Status status = isValid(url, user, password, persistPassword);

        // make sure a server with the same properties does not exist
        if (!status.isError()) {
            Server newServer = new Server(url, user, password, persistPassword);

            if (serverManager.isRegistered(newServer)) {
                status = new Status(Severity.ERROR, RestClientI18n.serverExistsMsg.text(newServer.getShortDescription()), null);
            }
        }

        return status;
    }

    // ===========================================================================================================================
    // Constructors
    // ===========================================================================================================================

    /**
     * Disallow construction.
     */
    private ServerValidator() {
        // nothing to do
    }

}
