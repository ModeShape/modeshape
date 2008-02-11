/*
 *
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
