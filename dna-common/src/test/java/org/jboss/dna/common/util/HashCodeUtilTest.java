/**
 * 
 */
package org.jboss.dna.common.util;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;
import org.jboss.dna.common.util.HashCodeUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Randall Hauch
 */
public class HashCodeUtilTest {

    @Before
    public void beforeEach() throws Exception {
    }

    @After
    public void afterEach() throws Exception {
    }

    @Test
    public void shouldComputeHashCodeForOnePrimitive() {
        assertThat(HashCodeUtil.computeHash(1), is(not(0)));
        assertThat(HashCodeUtil.computeHash((long)8), is(not(0)));
        assertThat(HashCodeUtil.computeHash((short)3), is(not(0)));
        assertThat(HashCodeUtil.computeHash(1.0f), is(not(0)));
        assertThat(HashCodeUtil.computeHash(1.0d), is(not(0)));
        assertThat(HashCodeUtil.computeHash(true), is(not(0)));
    }

    @Test
    public void shouldComputeHashCodeForMultiplePrimitives() {
        assertThat(HashCodeUtil.computeHash(1, 2, 3), is(not(0)));
        assertThat(HashCodeUtil.computeHash((long)8, (long)22, 33), is(not(0)));
        assertThat(HashCodeUtil.computeHash((short)3, (long)22, true), is(not(0)));
    }

    @Test
    public void shouldAcceptNoArguments() {
        assertThat(HashCodeUtil.computeHash(), is(0));
    }

    @Test
    public void shouldAcceptNullArguments() {
        assertThat(HashCodeUtil.computeHash((Object)null), is(0));
        assertThat(HashCodeUtil.computeHash("abc", (Object)null), is(not(0))); //$NON-NLS-1$
    }

}
