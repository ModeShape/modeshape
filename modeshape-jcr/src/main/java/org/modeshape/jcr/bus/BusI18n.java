package org.modeshape.jcr.bus;

import org.modeshape.common.annotation.Immutable;
import org.modeshape.common.i18n.I18n;

/**
 * The internationalized string constants for the <code>org.modeshape.repository.event.*</code> packages.
 *
 * @author Horia Chiorean
 */
@Immutable
public final class BusI18n {
    public static I18n errorSerializingChanges;
    public static I18n errorDeserializingChanges;

    static {
        try {
            I18n.initialize(BusI18n.class);
        } catch (final Exception err) {
            System.err.println(err);
        }
    }
}
