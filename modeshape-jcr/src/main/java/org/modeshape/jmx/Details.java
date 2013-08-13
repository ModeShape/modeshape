package org.modeshape.jmx;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.management.DescriptorKey;

/**
 * Annotation which adds JMX additional information via {@link DescriptorKey} to any MBean, method or parameter. This additional
 * information may be exposed by JMX clients, allowing better interaction with the MBean(s).
 *
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
@Documented
@Target( {ElementType.TYPE, ElementType.PARAMETER, ElementType.METHOD})
@Retention( RetentionPolicy.RUNTIME)
public @interface Details {
    @DescriptorKey("details")
    String value();
}
