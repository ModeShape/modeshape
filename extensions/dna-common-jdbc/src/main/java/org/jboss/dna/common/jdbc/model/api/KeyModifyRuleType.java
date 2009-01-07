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
package org.jboss.dna.common.jdbc.model.api;

import java.sql.DatabaseMetaData;

/**
 * Provides RDBMS supported keys update/delete rule types as enumeration set.
 * 
 * @author <a href="mailto:litsenko_sergey@yahoo.com">Sergiy Litsenko</a>
 */
public enum KeyModifyRuleType {

    CASCADE(DatabaseMetaData.importedKeyCascade), // when the primary key is updated/deleted, the foreign key (imported key) is
    // changed/deleted to agree with it;
    RESTRICT(DatabaseMetaData.importedKeyRestrict), // a primary key may not be updated/deleted if it has been imported by another
    // table as a foreign key.
    SET_NULL(DatabaseMetaData.importedKeySetNull), // when the primary key is updated or deleted, the foreign key (imported key)
    // is changed to <code>NULL</code>.
    NO_ACTION(DatabaseMetaData.importedKeyNoAction), // if the primary key has been imported, it cannot be updated or deleted.
    SET_DEFAULT(DatabaseMetaData.importedKeySetDefault); // if the primary key is updated or deleted, the foreign key (imported
    // key) is set to the default value.

    private final int rule;

    KeyModifyRuleType( int rule ) {
        this.rule = rule;
    }

    public int getRule() {
        return rule;
    }

    public String getName() {
        return name();
    }
}
