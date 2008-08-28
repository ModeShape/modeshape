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
import static org.junit.Assert.*;
import org.junit.Test;

/**
 * @author Serge Pagop
 *
 */
public class JcrUtilTest {

    /**
     * Test method for {@link org.jboss.dna.common.util.JcrUtil#createPathWithIndex(java.lang.String, int)}.
     */
    @Test
    public void shouldCreatePathWithIndex() {
        int index = 1;
        String path = JcrUtil.createPathWithIndex("/a/b/c", 1);
        char c = path.charAt(path.indexOf("[") + 1);
        assertTrue( c == Integer.toString(index).charAt(0));
    }

    /**
     * Test method for {@link org.jboss.dna.common.util.JcrUtil#createPath(java.lang.String)}.
     */
    @Test
    public void shouldCreatePath() {
       String path = JcrUtil.createPath("/a/b/c");
       assertTrue(path.length() > 0);
       assertThat(path, is(new String("/a/b/c")));
    }

}
