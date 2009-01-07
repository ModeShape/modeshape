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
package org.jboss.example.dna.sequencers;

import java.util.Map;
import java.util.Properties;
import net.jcip.annotations.Immutable;

/**
 * @author Randall Hauch
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
