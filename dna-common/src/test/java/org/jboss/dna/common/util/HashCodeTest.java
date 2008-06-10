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

package org.jboss.dna.common.util;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;
import org.junit.Test;

/**
 * @author Randall Hauch
 */
public class HashCodeTest {

    @Test
    public void shouldComputeHashCodeForOnePrimitive() {
        assertThat(HashCode.compute(1), is(not(0)));
        assertThat(HashCode.compute((long)8), is(not(0)));
        assertThat(HashCode.compute((short)3), is(not(0)));
        assertThat(HashCode.compute(1.0f), is(not(0)));
        assertThat(HashCode.compute(1.0d), is(not(0)));
        assertThat(HashCode.compute(true), is(not(0)));
    }

    @Test
    public void shouldComputeHashCodeForMultiplePrimitives() {
        assertThat(HashCode.compute(1, 2, 3), is(not(0)));
        assertThat(HashCode.compute((long)8, (long)22, 33), is(not(0)));
        assertThat(HashCode.compute((short)3, (long)22, true), is(not(0)));
    }

    @Test
    public void shouldAcceptNoArguments() {
        assertThat(HashCode.compute(), is(0));
    }

    @Test
    public void shouldAcceptNullArguments() {
        assertThat(HashCode.compute((Object)null), is(0));
        assertThat(HashCode.compute("abc", (Object)null), is(not(0))); //$NON-NLS-1$
    }

}
