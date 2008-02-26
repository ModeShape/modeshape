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

package org.jboss.dna.common.junit;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.jboss.dna.common.junit.IsCloseTo.closeTo;
import org.junit.Test;

/**
 * @author Randall Hauch
 */
public class IsCloseToTest {

    @Test( expected = AssertionError.class )
    public void shouldFailWhenNumberIsNotCloserToValue() {
        assertThat(100.1d, is(closeTo(100.0d, 0.001d)));
    }

    @Test
    public void shouldPassWhenNumberIsCloserToValue() {
        assertThat(100.0001d, is(closeTo(100.0d, 0.001d)));
        assertThat(-100.0001d, is(closeTo(-100.0d, 0.001d)));
    }

}
