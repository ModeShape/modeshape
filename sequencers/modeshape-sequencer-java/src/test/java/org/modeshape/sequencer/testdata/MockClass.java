/*
 * ModeShape (http://www.modeshape.org)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * ModeShape is free software. Unless otherwise indicated, all code in ModeShape
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * ModeShape is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org
 */
package org.modeshape.sequencer.testdata;

import java.io.Serializable;
import java.util.*;

/**
 * Dummy class, used for testing the sequencing.
 * 
 * @author Horia Chiorean
 */
public final class MockClass implements Serializable {

    private static final long serialVersionUID = 1L;

    @SuppressWarnings( "unused" )
    private static volatile String STATIC_VOLATILE_STRING_FIELD;
    public static final Integer STATIC_FINAL_INTEGER_FIELD = 0;

    protected Boolean booleanField;

    // The order of these constructors and methods is important, because they reflect the same order as represented in the class
    // file. While this has no bearing on the functionality of the sequencer (which just processes the methods in the same
    // order as they appear), having the same order in the source and class files makes it easier for our tests
    // to check expected results.

    public MockClass() {
        this.booleanField = Boolean.FALSE;
    }

    public MockClass( boolean booleanField ) {
        this.booleanField = booleanField;
    }

    public MockClass( Boolean booleanField ) {
        this.booleanField = booleanField;
    }

    public void doSomething( double p1 ) {
    }

    public void doSomething( float p1 ) {
        List<String> list = new ArrayList<String>();
        list.add("string");
    }

    /**
     * Sets the boolean field to the default value. (This is not idiomatic Java, but I'm doing this just to have overloaded
     * methods.)
     */
    public void setField() {
        this.booleanField = Boolean.FALSE;
    }

    /**
     * @param booleanField Sets booleanField to the specified value.
     */
    public void setField( Boolean booleanField ) {
        this.booleanField = booleanField;
    }

    @Deprecated
    synchronized void voidMethod() {
    }

}
