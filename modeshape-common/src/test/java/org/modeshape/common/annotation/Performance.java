package org.modeshape.common.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.Ignore;
import org.junit.Test;

/**
 * An annotation that describes a test as for measuring performance. Such tests often take significantly longer than most unit
 * tests, and so they are often {@link Ignore}d most of the time and run only manually. </p>
 * 
 * @see Test
 * @see Ignore
 */
@Documented
@Target( {ElementType.FIELD, ElementType.METHOD} )
@Retention( RetentionPolicy.SOURCE )
public @interface Performance {
}
