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

import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import net.jcip.annotations.Immutable;

/**
 * The information about Java source.
 */
@Immutable
public class JavaInfo extends ContentInfo {

    private final Map<String, List<Properties>> javaElements;
    private final String type;

    protected JavaInfo( String path,
                        String name,
                        String type,
                        Map<String, List<Properties>> javaElements ) {
        super(path, name, null);
        this.type = type;
        this.javaElements = javaElements != null ? new TreeMap<String, List<Properties>>(javaElements) : new TreeMap<String, List<Properties>>();
    }

    public String getType() {
        return this.type;
    }

    /**
     * @return javaElements
     */
    public Map<String, List<Properties>> getJavaElements() {
        return javaElements;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("   Name: " + getName() + "\n");
        sb.append("   Path: " + getPath() + "\n");
        sb.append("   Type: " + getType() + "\n");
        for (Map.Entry<Object, Object> entry : getProperties().entrySet()) {
            sb.append("   " + entry.getKey() + ": " + entry.getValue() + "\n");
        }
        for (Map.Entry<String, List<Properties>> javaElement : getJavaElements().entrySet()) {
            sb.append("\n   ------ " + javaElement.getKey() + " ------\n");
            for (Properties props : javaElement.getValue()) {
                for (Map.Entry<Object, Object> entry : props.entrySet()) {
                    if (!entry.getKey().equals("jcr:primaryType")) {
                        sb.append("   " + entry.getKey() + " => " + entry.getValue() + "\n");
                    }
                }
            }
        }
        return sb.toString();
    }

}
