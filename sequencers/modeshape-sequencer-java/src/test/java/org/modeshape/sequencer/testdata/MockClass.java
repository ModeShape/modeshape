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
package org.modeshape.sequencer.testdata;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

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
