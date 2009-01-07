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
package org.jboss.dna.graph.properties;

import java.math.BigDecimal;
import org.jboss.dna.graph.properties.ValueComparators;

/**
 * @author Randall Hauch
 */
public class DecimalValueComparatorTest extends AbstractValueComparatorsTest<BigDecimal> {

    public DecimalValueComparatorTest() {
        super(ValueComparators.DECIMAL_COMPARATOR, new BigDecimal(100), new BigDecimal(200.0), new BigDecimal(300.0), new BigDecimal(-1.0), new BigDecimal(0.0));
    }
}
