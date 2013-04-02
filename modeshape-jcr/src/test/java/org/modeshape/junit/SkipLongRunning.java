package org.modeshape.junit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marker annotation used together with the {@link SkipLongRunningRule} JUnit rule, that allows long running tests to be excluded
 * from the build, using the {@code skipLongRunningTests} system property.
 *
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
@Retention( RetentionPolicy.RUNTIME)
@Target( {ElementType.METHOD, ElementType.TYPE})
public @interface SkipLongRunning {
}
