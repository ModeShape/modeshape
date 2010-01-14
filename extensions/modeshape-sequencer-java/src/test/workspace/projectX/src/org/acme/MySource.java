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
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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
    
    private double doSomething2(Object[] oa, int[] ia) {
        System.out.println("genial");
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
