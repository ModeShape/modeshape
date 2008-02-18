/*
 * JBoss, Home of Professional Open Source. Copyright 2008, Red Hat Middleware LLC, and individual contributors as indicated by
 * the @author tags. See the copyright.txt file in the distribution for a full listing of individual contributors. This is free
 * software; you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation; either version 2.1 of the License, or (at your option) any later version. This software is
 * distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details. You should have received a
 * copy of the GNU Lesser General Public License along with this software; if not, write to the Free Software Foundation, Inc., 51
 * Franklin St, Fifth Floor, Boston, MA 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.common.util;

import org.junit.Test;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * @author Randall Hauch
 */
public class ClassUtilTest {

    @Test
    public void shouldConsiderValidClassnamesToBeValid() {
        assertValidClassname(this.getClass().getName());
        assertValidClassname("org.jboss.a.b.c.d.e.f.SomeClass");
        assertValidClassname("SomeClass");
    }

    @Test
    public void shouldConsiderInvalidClassnamesToBeInvalid() {
        assertNotValidClassname(null);
        assertNotValidClassname("");
        assertNotValidClassname("  " + this.getClass().getName() + "  \t ");
        assertNotValidClassname("1.2.3.4");
        assertNotValidClassname("org.jboss.a.b.c.d.e.f.2SomeClass");
        assertNotValidClassname("org/jboss/a/b/c/d/e.f.SomeClass");
    }

    protected void assertValidClassname( String classname ) {
        assertThat(ClassUtil.isFullyQualifiedClassname(classname), is(true));
    }

    protected void assertNotValidClassname( String classname ) {
        assertThat(ClassUtil.isFullyQualifiedClassname(classname), is(false));
    }

}
