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
package org.jboss.example.dna.sequencers;

import java.util.Map;
import java.util.Properties;

/**
 * @author Randall Hauch
 */
public class ImageInfo {

    private final Properties properties = new Properties();
    private final String name;
    private final String path;

    protected ImageInfo( String path, String name, Properties props ) {
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

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Map.Entry<Object, Object> entry : this.properties.entrySet()) {
            sb.append(entry.getKey()).append("=>").append(entry.getValue());
            if (first) {
                first = false;
            } else {
                sb.append(", ");
            }
        }
        return this.name + " (at " + this.path + ") with properties {" + sb.toString() + "}";
    }

}
