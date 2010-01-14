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

package org.modeshape.sequencer.zip;

import java.util.ArrayList;
import java.util.List;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.NamespaceRegistry;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.ValueFactories;
import org.modeshape.graph.sequencer.SequencerOutput;
import org.junit.Ignore;

/**
 * @author Michael Trezzi
 */
@Ignore
public class SequencingOutputTestClass implements SequencerOutput {
    List<PropertyClassTest> properties = new ArrayList<PropertyClassTest>();
    List<PropertyClassTest> references = new ArrayList<PropertyClassTest>();

    public ValueFactories getFactories() {
        return null;
    }

    public NamespaceRegistry getNamespaceRegistry() {
        return null;
    }

    public void setProperty( String nodePath,
                             String propertyName,
                             Object... values ) {
        System.out.println("Setting property on '" + nodePath + "' " + propertyName + ":" + values[0]);
        properties.add(new PropertyClassTest(nodePath, propertyName, values[0]));
    }

    public void setReference( String nodePath,
                              String propertyName,
                              String... paths ) {
        System.out.println("Setting reference on " + nodePath + " " + propertyName + ":" + paths[0]);
        references.add(new PropertyClassTest(nodePath, propertyName, paths[0]));
    }

    public void setProperty( Path nodePath,
                             Name propertyName,
                             Object... values ) {
        System.out.println("Setting property on " + nodePath.getString() + " " + propertyName.getString() + ":" + values[0]);
        properties.add(new PropertyClassTest(nodePath.getString(), propertyName.getString(), values[0]));
    }
}
