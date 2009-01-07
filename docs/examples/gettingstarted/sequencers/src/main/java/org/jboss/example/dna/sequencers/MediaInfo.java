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
public class MediaInfo extends ContentInfo {

    private final String mediaType;

    protected MediaInfo( String path,
                         String name,
                         String mediaType,
                         Properties props ) {
        super(path, name, props);
        this.mediaType = mediaType;
    }

    public String getMediaType() {
        return this.mediaType;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("   Name: " + getName() + "\n");
        sb.append("   Path: " + getPath() + "\n");
        sb.append("   Type: " + getMediaType() + "\n");
        for (Map.Entry<Object, Object> entry : getProperties().entrySet()) {
            sb.append("   " + entry.getKey() + ": " + entry.getValue() + "\n");
        }
        return sb.toString();
    }

}
