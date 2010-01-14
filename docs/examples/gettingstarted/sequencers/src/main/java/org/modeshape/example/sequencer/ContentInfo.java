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
package org.modeshape.example.sequencer;

import java.util.Map;
import java.util.Properties;
import net.jcip.annotations.Immutable;

/**
 * The information about content.
 */
@Immutable
public class ContentInfo {

    private final Properties properties = new Properties();
    private final String name;
    private final String path;

    protected ContentInfo( String path,
                           String name,
                           Properties props ) {
        this.name = name;
        this.path = path;
        if (props != null) this.properties.putAll(props);
    }

    public String getName() {
        return this.name;
    }

    public String getPath() {
        return this.path;
    }

    public Properties getProperties() {
        return this.properties;
    }

    public String getInfoType() {
        return this.getClass().getSimpleName().replaceAll("Info$", "");
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("   Name: " + getName() + "\n");
        sb.append("   Path: " + getPath() + "\n");
        for (Map.Entry<Object, Object> entry : getProperties().entrySet()) {
            sb.append("   " + entry.getKey() + ": " + entry.getValue() + "\n");
        }
        return sb.toString();
    }

}
