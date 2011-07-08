package org.modeshape.jdbc.util;

import org.junit.Test;

public class StringUtilTest {

    @Test( expected = IllegalArgumentException.class )
    public void createStringShouldFailIfTooFewArgumentsSupplied() {
        String pattern = "This {0} is {1} should {2} not {3} last {4}";
        try {
            StringUtil.createString(pattern, "one", "two", "three", "four");
        } catch (IllegalArgumentException err) {
            System.err.println(err);
            throw err;
        }
    }

    @Test( expected = IllegalArgumentException.class )
    public void createStringShouldFailIfTooManyArgumentsSupplied() {
        String pattern = "This {0} is {1} should {2} not {3} last {4}";
        try {
            StringUtil.createString(pattern, "one", "two", "three", "four", "five", "six");
        } catch (IllegalArgumentException err) {
            System.err.println(err);
            throw err;
        }
    }

}
