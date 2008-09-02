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

import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

/**
 * @author Serge Pagop
 */
public class JavaInfo {

    private final Map<String, List<Properties>> javaElements = new TreeMap<String, List<Properties>>();

    private final String name;
    private final String path;
    private final String type;

    protected JavaInfo( String path,
                        String name,
                        String type,
                        Map<String, List<Properties>> javaElements ) {
        this.name = name;
        this.path = path;
        this.type = type;
        if (javaElements != null) this.javaElements.putAll(javaElements);
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

        for (Map.Entry<String, List<Properties>> javaElement : getJavaElements().entrySet()) {
            sb.append("\n------ " + javaElement.getKey() + " ------\n");
            for (Properties props : javaElement.getValue()) {
                for (Map.Entry<Object, Object> entry : props.entrySet()) {
                    if (!entry.getKey().equals("jcr:primaryType")) {
                        sb.append(entry.getKey()).append(" => ").append(entry.getValue());
                        sb.append("; ");
                    }
                }
            }
        }

        return this.name + " (at " + this.path + ") of type \"" + this.type + "\" with elements \n{\n" + sb.toString()
               + " \n}\n";
    }

    /**
     * @return javaElements
     */
    public Map<String, List<Properties>> getJavaElements() {
        return javaElements;
    }

}
