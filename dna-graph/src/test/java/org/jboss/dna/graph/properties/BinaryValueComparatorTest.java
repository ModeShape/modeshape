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
package org.jboss.dna.graph.properties;

import org.jboss.dna.graph.properties.Binary;
import org.jboss.dna.graph.properties.ValueComparators;
import org.jboss.dna.graph.properties.ValueFactory;

/**
 * @author Randall Hauch
 */
public class BinaryValueComparatorTest extends AbstractValueComparatorsTest<Binary> {

    private static final ValueFactory<Binary> FACTORY;
    private static final Binary BINARY1;
    private static final Binary BINARY2;
    private static final Binary BINARY3;

    static {
        FACTORY = VALUE_FACTORIES.getBinaryFactory();
        BINARY1 = FACTORY.create("This is the binary 1 value");
        BINARY2 = FACTORY.create("This is the binary 2 value");
        BINARY3 = FACTORY.create("This is the binary 3 value");
    }

    public BinaryValueComparatorTest() {
        super(ValueComparators.BINARY_COMPARATOR, BINARY1, BINARY2, BINARY3);
    }
}
