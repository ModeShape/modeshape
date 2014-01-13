/*
 * ModeShape (http://www.modeshape.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.modeshape.common.util;

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
