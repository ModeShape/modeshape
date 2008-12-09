/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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

import java.sql.ResultSet;

/**
 * Provides all supported database result set types as enumeration set.
 * 
 * @author <a href="mailto:litsenko_sergey@yahoo.com">Sergiy Litsenko</a>
 * @since 1.2 (JDBC 2.0)
 */
public enum ResultSetConcurrencyType {

    READ_ONLY(ResultSet.CONCUR_READ_ONLY),
    UPDATABLE(ResultSet.CONCUR_UPDATABLE);

    private final int concurrency;

    ResultSetConcurrencyType( int concurrency ) {
        this.concurrency = concurrency;
    }

    public int getConcurrency() {
        return concurrency;
    }

    public String getName() {
        return name();
    }
}
