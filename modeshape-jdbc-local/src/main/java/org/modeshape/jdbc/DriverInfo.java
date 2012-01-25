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
package org.modeshape.jdbc;

/**
 * Information about the driver.
 */
public final class DriverInfo {

    private final String name;
    private final String vendorName;
    private final String vendorUrl;
    private final String version;
    private final int majorVersion;
    private final int minorVersion;

    public DriverInfo( String name,
                       String vendorName,
                       String vendorUrl,
                       String version ) {
        this.name = name;
        this.vendorName = vendorName;
        this.vendorUrl = vendorUrl;
        this.version = version;
        String[] coords = getVersion().split("[.-]");
        this.majorVersion = coords.length > 0 && coords[0] != null ? Integer.parseInt(coords[0]) : 0;
        this.minorVersion = coords.length > 1 && coords[1] != null ? Integer.parseInt(coords[1]) : 0;
    }

    public String getVendorName() {
        return vendorName;
    }

    public String getVendorUrl() {
        return vendorUrl;
    }

    public String getVersion() {
        return version;
    }

    public int getMajorVersion() {
        return majorVersion;
    }

    public int getMinorVersion() {
        return minorVersion;
    }

    public String getName() {
        return name;
    }
}
