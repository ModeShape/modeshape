/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008-2009, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors. 
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.common.jdbc.model.spi;

import org.jboss.dna.common.jdbc.model.api.Privilege;
import org.jboss.dna.common.jdbc.model.api.PrivilegeType;

/**
 * Provides all database privilege specific metadata.
 * 
 * @author <a href="mailto:litsenko_sergey@yahoo.com">Sergiy Litsenko</a>
 */
public class PrivilegeBean extends CoreMetaDataBean implements Privilege {
    private static final long serialVersionUID = -163129768802977718L;
    private PrivilegeType privilegeType;
    private String grantor;
    private String grantee;
    private String name;
    private Boolean grantable;
    private Boolean unknownGrantable;

    /**
     * Default constructor
     */
    public PrivilegeBean() {
    }

    /**
     * Returns privilege type
     * 
     * @return privilege type
     */
    public PrivilegeType getPrivilegeType() {
        return privilegeType;
    }

    /**
     * Sets privilege type
     * 
     * @param privilegeType the privilege type
     */
    public void setPrivilegeType( PrivilegeType privilegeType ) {
        this.privilegeType = privilegeType;
    }

    /**
     * Return grantor of access (may be <code>null</code>)
     * 
     * @return grantor of access (may be <code>null</code>)
     */
    public String getGrantor() {
        return grantor;
    }

    /**
     * Sets grantor of access (may be <code>null</code>)
     * 
     * @param grantor the grantor of access (may be <code>null</code>)
     */
    public void setGrantor( String grantor ) {
        this.grantor = grantor;
    }

    /**
     * Return grantee of access (may be <code>null</code>)
     * 
     * @return grantee of access (may be <code>null</code>)
     */
    public String getGrantee() {
        return grantee;
    }

    /**
     * Sets grantee of access (may be <code>null</code>)
     * 
     * @param grantee the grantee of access (may be <code>null</code>)
     */
    public void setGrantee( String grantee ) {
        this.grantee = grantee;
    }

    /**
     * Return name of access allowed (SELECT, INSERT, UPDATE, REFRENCES, ...)
     * 
     * @return name of access allowed (SELECT, INSERT, UPDATE, REFRENCES, ...)
     */
    public String getName() {
        return name;
    }

    /**
     * Sets name of access allowed (SELECT, INSERT, UPDATE, REFRENCES, ...)
     * 
     * @param name the name of access allowed (SELECT, INSERT, UPDATE, REFRENCES, ...)
     */
    public void setName( String name ) {
        this.name = name;
    }

    /**
     * Return true if grantee is permitted to grant to others, false otherwise (even if unknown).
     * 
     * @return true if grantee is permitted to grant to others, false otherwise (even if unknown).
     */
    public Boolean isGrantable() {
        return grantable;
    }

    /**
     * Sets true if grantee is permitted to grant to others, false otherwise (even if unknown).
     * 
     * @param grantable true if grantee is permitted to grant to others, false otherwise (even if unknown).
     */
    public void setGrantable( Boolean grantable ) {
        this.grantable = grantable;
    }

    /**
     * Return true if it is unknown: grantee is permitted to grant to others or not
     * 
     * @return true if it is unknown: grantee is permitted to grant to others or not
     */
    public Boolean isUnknownGrantable() {
        return unknownGrantable;
    }

    /**
     * sets true if it is unknown: grantee is permitted to grant to others or not
     * 
     * @param unknownGrantable true if it is unknown: grantee is permitted to grant to others or not
     */
    public void setUnknownGrantable( Boolean unknownGrantable ) {
        this.unknownGrantable = unknownGrantable;
    }
}
