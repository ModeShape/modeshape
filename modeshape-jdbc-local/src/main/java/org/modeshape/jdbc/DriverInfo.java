/*
 * ModeShape (http://www.modeshape.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
