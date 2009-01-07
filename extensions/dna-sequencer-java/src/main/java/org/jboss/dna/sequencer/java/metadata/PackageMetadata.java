/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008-2009, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.dna.sequencer.java.metadata;

import java.util.ArrayList;
import java.util.List;

/**
 * Package meta data.
 * 
 * @author Serge Pagop.
 */
public class PackageMetadata {
    private JavadocMetadata javadocMetadata;
    private List<AnnotationMetadata> annotationMetada = new ArrayList<AnnotationMetadata>();
    private String name;

    // No-Arg
    public PackageMetadata() {
    }

    public PackageMetadata( String name ) {
        this.name = name;
    }

    public void setName( String name ) {
        this.name = name;

    }

    public String getName() {
        return this.name;
    }

    /**
     * @return annotationMetada
     */
    public List<AnnotationMetadata> getAnnotationMetada() {
        return annotationMetada;
    }

    /**
     * @param annotationMetada Sets annotationMetada to the specified value.
     */
    public void setAnnotationMetada( List<AnnotationMetadata> annotationMetada ) {
        this.annotationMetada = annotationMetada;
    }

    /**
     * @return javadocMetadata
     */
    public JavadocMetadata getJavadocMetadata() {
        return javadocMetadata;
    }

    /**
     * @param javadocMetadata Sets javadocMetadata to the specified value.
     */
    public void setJavadocMetadata( JavadocMetadata javadocMetadata ) {
        this.javadocMetadata = javadocMetadata;
    }

}
