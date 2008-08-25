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
package org.jboss.dna.sequencer.java;

import java.util.Map;
import java.util.Properties;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

/**
 * @author Serge Pagop
 *
 */
public class JavaInfo {
    
    private final  Multimap<String, String> map =  new ArrayListMultimap<String, String>();
    
    private final String name;
    private final String path;
    private final String type;

    protected JavaInfo( String path, String name, String type, Multimap<String, String> map) {
        this.name = name;
        this.path = path;
        this.type = type;
        if (map != null) this.map.putAll(map);
    }

    public String getName() {
        return this.name;
    }

    public String getPath() {
        return this.path;
    }

    public String getType() {
        return this.type;
    }



    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> entry : map.entries()) {
            sb.append(entry.getKey()).append("=>").append(entry.getValue());
            if (first) {
                first = false;
            } else {
                sb.append(", ");
            }
        }
        return this.name + " (at " + this.path + ") of type \"" + this.type + "\" with properties {" + sb.toString() + "}";
    }

    /**
     * @return map
     */
    public Multimap<String, String> getMap() {
        return map;
    }

}
