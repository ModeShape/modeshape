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

package org.jboss.dna.sequencer.zip;

import org.jboss.dna.spi.sequencers.SequencerOutput;
import org.jboss.dna.spi.graph.*;
import org.junit.Test;
import org.junit.Ignore;

import java.util.Collection;
import java.util.List;
import java.util.ArrayList;

/**
 * @author Michael Trezzi
 */
@Ignore
public class SequencingOutputTestClass implements SequencerOutput {
    List<PropertyClassTest> properties = new ArrayList();
    List<PropertyClassTest> references = new ArrayList();



    public ValueFactories getFactories() {
        return null;
    }
    
    public NamespaceRegistry getNamespaceRegistry() {
        return null;
    }

    public void setProperty(String nodePath, String propertyName, Object... values) {
        System.out.println("Setting property on '"+nodePath+"' "+propertyName+":"+values[0]);
        properties.add(new PropertyClassTest(nodePath,propertyName,values[0]));
    }

    public void setReference(String nodePath, String propertyName, String... paths) {
        System.out.println("Setting reference on "+nodePath+" "+propertyName+":"+paths[0]);
        references.add(new PropertyClassTest(nodePath,propertyName,paths[0]));
    }

    public void setProperty(Path nodePath, Name propertyName, Object... values) {
        System.out.println("Setting property on "+nodePath.getString()+" "+propertyName.getString()+":"+values[0]);
        properties.add(new PropertyClassTest(nodePath.getString(),propertyName.getString(),values[0]));
    }
}
