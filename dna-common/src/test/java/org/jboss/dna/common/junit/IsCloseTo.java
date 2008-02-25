/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors. 
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.dna.common.junit;

import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.junit.matchers.TypeSafeMatcher;

/**
 * @author Randall Hauch
 * @param <T>
 */
public class IsCloseTo<T extends Number> extends TypeSafeMatcher<T> {

    private final T value;
    private final T precision;

    public IsCloseTo( T value, T precision ) {
        this.value = value;
        this.precision = precision;
    }

    /**
     * {@inheritDoc}
     */
    public void describeTo( Description description ) {
        description.appendText("not close to");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean matchesSafely( T value ) {
        return Math.abs(value.doubleValue() - this.value.doubleValue()) <= this.precision.doubleValue();
    }

    @SuppressWarnings( "unchecked" )
    @Factory
    public static <T> Matcher<T> closeTo( double value, double precision ) {
        return new IsCloseTo(value, Math.abs(precision));
    }

    @SuppressWarnings( "unchecked" )
    @Factory
    public static <T> Matcher<T> closeTo( float value, float precision ) {
        return new IsCloseTo(value, Math.abs(precision));
    }
}
