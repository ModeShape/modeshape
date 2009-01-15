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
package org.jboss.dna.common.jdbc.model.api;

/**
 * Provides all database privilege specific metadata.
 * 
 * @author <a href="mailto:litsenko_sergey@yahoo.com">Sergiy Litsenko</a>
 */
public interface Privilege extends CoreMetaData {

    /**
     * Returns privilege type
     * 
     * @return privilege type
     */
    PrivilegeType getPrivilegeType();

    /**
     * Sets privilege type
     * 
     * @param privilegeType the privilege type
     */
    void setPrivilegeType( PrivilegeType privilegeType );

    /**
     * Return grantor of access (may be <code>null</code>)
     * 
     * @return grantor of access (may be <code>null</code>)
     */
    String getGrantor();

    /**
     * Sets grantor of access (may be <code>null</code>)
     * 
     * @param grantor the grantor of access (may be <code>null</code>)
     */
    void setGrantor( String grantor );

    /**
     * Return grantee of access (may be <code>null</code>)
     * 
     * @return grantee of access (may be <code>null</code>)
     */
    String getGrantee();

    /**
     * Sets grantee of access (may be <code>null</code>)
     * 
     * @param grantee the grantee of access (may be <code>null</code>)
     */
    void setGrantee( String grantee );

    /**
     * Return name of access allowed (SELECT, INSERT, UPDATE, REFRENCES, ...)
     * 
     * @return name of access allowed (SELECT, INSERT, UPDATE, REFRENCES, ...)
     */
    String getName();

    /**
     * Sets name of access allowed (SELECT, INSERT, UPDATE, REFRENCES, ...)
     * 
     * @param name the name of access allowed (SELECT, INSERT, UPDATE, REFRENCES, ...)
     */
    void setName( String name );

    /**
     * Return true if grantee is permitted to grant to others, false otherwise (even if unknown).
     * 
     * @return true if grantee is permitted to grant to others, false otherwise (even if unknown).
     */
    Boolean isGrantable();

    /**
     * Sets true if grantee is permitted to grant to others, false otherwise (even if unknown).
     * 
     * @param grantable true if grantee is permitted to grant to others, false otherwise (even if unknown).
     */
    void setGrantable( Boolean grantable );

    /**
     * Return true if it is unknown: grantee is permitted to grant to others or not
     * 
     * @return true if it is unknown: grantee is permitted to grant to others or not
     */
    Boolean isUnknownGrantable();

    /**
     * sets true if it is unknown: grantee is permitted to grant to others or not
     * 
     * @param unknownGrantable true if it is unknown: grantee is permitted to grant to others or not
     */
    void setUnknownGrantable( Boolean unknownGrantable );
}
