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
package org.acme;

import org.acme.annotation.MyClassAnnotation;
import java.util.*;

/**
 * @author Serge Pagop
 */
@MyClassAnnotation
public class MySource {

    private int i, j;
    private static double a;
    private List<String> l;
    private A<Integer> o;
    private X x;
    private int[] ia;
    private Object[] oa;
    private Collection[] ca;

    MySource() {
    }
    public MySource(int i, int j) {
        this.i = i;
        this.j = j;
    }
    
    public MySource(int i, int j, Object o) {
        this.i = i;
        this.j = j;
    }
    

    public int getI() {
        return this.i;
    }

    public void setI( int i ) {
        this.i = i;
    }

    public void setJ( int j ) {
        this.j = j;
    }

    public void doSomething(int p1, double p2, Object o) {
        l = new ArrayList<String>();
        l.add("N1");
    }
    
    public void doSomething(int p1, double p2, float p3, Object o) {
        l = new ArrayList<String>();
        l.add("N1");
    }
    
    private double doSomething2(Object[] oa, int[] ia) {
        System.out.println("genial");
        return 1.0;
    }
    
    public Object doSomething3() {
        return null;
    }

    // nested class
    class A<E> {
        E e;

        A( E e ) {
            this.e = e;
        }

        A() {
            this.e = null;
        }

        @Override
        public String toString() {
            return String.valueOf(this.e);
        }

        class B<T> {
            T t;

            B( T t ) {
                this.t = t;
            }

            @Override
            public String toString() {
                return String.valueOf(this.t);
            }
        }
    }

    class X {

    }
}
