/*
 *
 */
package org.jboss.dna.common.collection;

import java.util.Iterator;
import org.jboss.dna.common.i18n.I18n;

/**
 * @author John Verhaeg
 */
public interface Problems extends Iterable<Problem> {

    void addError( I18n message,
                   Object... params );

    void addError( Throwable throwable,
                   I18n message,
                   Object... params );

    void addError( I18n message,
                   String resource,
                   String location,
                   Object... params );

    void addError( Throwable throwable,
                   I18n message,
                   String resource,
                   String location,
                   Object... params );

    void addError( int code,
                   I18n message,
                   Object... params );

    void addError( Throwable throwable,
                   int code,
                   I18n message,
                   Object... params );

    void addError( int code,
                   I18n message,
                   String resource,
                   String location,
                   Object... params );

    void addError( Throwable throwable,
                   int code,
                   I18n message,
                   String resource,
                   String location,
                   Object... params );

    void addWarning( I18n message,
                     Object... params );

    void addWarning( Throwable throwable,
                     I18n message,
                     Object... params );

    void addWarning( I18n message,
                     String resource,
                     String location,
                     Object... params );

    void addWarning( Throwable throwable,
                     I18n message,
                     String resource,
                     String location,
                     Object... params );

    void addWarning( int code,
                     I18n message,
                     Object... params );

    void addWarning( Throwable throwable,
                     int code,
                     I18n message,
                     Object... params );

    void addWarning( int code,
                     I18n message,
                     String resource,
                     String location,
                     Object... params );

    void addWarning( Throwable throwable,
                     int code,
                     I18n message,
                     String resource,
                     String location,
                     Object... params );

    void addInfo( I18n message,
                  Object... params );

    void addInfo( Throwable throwable,
                  I18n message,
                  Object... params );

    void addInfo( I18n message,
                  String resource,
                  String location,
                  Object... params );

    void addInfo( Throwable throwable,
                  I18n message,
                  String resource,
                  String location,
                  Object... params );

    void addInfo( int code,
                  I18n message,
                  Object... params );

    void addInfo( Throwable throwable,
                  int code,
                  I18n message,
                  Object... params );

    void addInfo( int code,
                  I18n message,
                  String resource,
                  String location,
                  Object... params );

    void addInfo( Throwable throwable,
                  int code,
                  I18n message,
                  String resource,
                  String location,
                  Object... params );

    boolean hasProblems();

    boolean hasErrors();

    boolean hasWarnings();

    boolean hasInfo();

    boolean isEmpty();

    int size();

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     * 
     * @see java.lang.Iterable#iterator()
     */
    Iterator<Problem> iterator();
}
