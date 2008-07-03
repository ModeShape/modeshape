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
package org.jboss.dna.sequencer.java;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.jboss.dna.common.monitor.ProgressMonitor;
import org.jboss.dna.sequencer.java.metadata.AnnotationMetadata;
import org.jboss.dna.sequencer.java.metadata.JavaMetadata;
import org.jboss.dna.sequencer.java.metadata.MarkerAnnotationMetadata;
import org.jboss.dna.sequencer.java.metadata.NormalAnnotationMetadata;
import org.jboss.dna.sequencer.java.metadata.PackageMetadata;
import org.jboss.dna.sequencer.java.metadata.SingleMemberAnnotationMetadata;
import org.jboss.dna.spi.graph.NameFactory;
import org.jboss.dna.spi.graph.Path;
import org.jboss.dna.spi.graph.PathFactory;
import org.jboss.dna.spi.sequencers.SequencerContext;
import org.jboss.dna.spi.sequencers.SequencerOutput;
import org.jboss.dna.spi.sequencers.StreamSequencer;

/**
 * A sequencer that processes a compilation unit, extracts the meta data for the compilation unit, and then writes these
 * informations to the repository.
 * <p>
 * The structural representation of the informations from the compilation unit looks like this:
 * <ul>
 * <li><strong>java:compilationUnit</strong> node of type <code>java:compilationUnit</code>
 * <ul>
 * <li><strong>java:package</strong> - optional child node that represents the package declaration of the compilation unit</li>
 * </ul>
 * </li>
 * </ul>
 * </p>
 * 
 * @author Serge Pagop
 */
public class JavaMetadataSequencer implements StreamSequencer {

    public static final String JAVA_COMPILATION_UNIT_NODE = "java:compilationUnit";
    public static final String JAVA_COMPILATION_UNIT_PRIMARY_TYPE = "jcr:primaryType";
    public static final String JAVA_PACKAGE_CHILD_NODE = "java:package";
    public static final String JAVA_PACKAGE_DECLARATION_CHILD_NODE = "java:packageDeclaration";
    public static final String JAVA_PACKAGE_NAME = "java:packageName";
    
    // Annnotation declaration
    public static final String JAVA_ANNOTATION_CHILD_NODE = "java:annotation";
    public static final String JAVA_ANNOTATION_DECLARATION_CHILD_NODE = "java:annotationDeclaration";
    public static final String JAVA_ANNOTATION_TYPE_CHILD_NODE = "java:annotationType";
    public static final String JAVA_MARKER_ANNOTATION_CHILD_NODE = "java:markerAnnotation";
    public static final String JAVA_NORMAL_ANNOTATION_CHILD_NODE = "java:normalAnnotation";
    public static final String JAVA_SINGLE_ELEMENT_ANNOTATION_CHILD_NODE = "java:singleElementAnnotation";
    public static final String JAVA_ANNOTATION_TYPE_NAME = "java:typeName";

    // Import declaration
    public static final String JAVA_IMPORT_CHILD_NODE = "java:import";
    public static final String JAVA_IMPORT_DECLARATION_CHILD_NODE = "java:importDeclaration";
    public static final String JAVA_SINGLE_IMPORT_CHILD_NODE = "java:singleImport";
    public static final String JAVA_SINGLE_TYPE_IMPORT_DECLARATION_CHILD_NODE = "java:singleTypeImportDeclaration";
    
    public static final String JAVA_IMPORT_ON_DEMAND_CHILD_NODE = "java:importOnDemand";
    public static final String JAVA_TYPE_IMPORT_ON_DEMAND_DECLARATION_CHILD_NODE = "java:typeImportOnDemandDeclaration";
    public static final String JAVA_IMPORT_TYPE_NAME = "typeName";

    private static final String SLASH = "/";
    
    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.spi.sequencers.StreamSequencer#sequence(java.io.InputStream,
     *      org.jboss.dna.spi.sequencers.SequencerOutput, org.jboss.dna.spi.sequencers.SequencerContext,
     *      org.jboss.dna.common.monitor.ProgressMonitor)
     */
    public void sequence( InputStream stream,
                          SequencerOutput output,
                          SequencerContext context,
                          ProgressMonitor progressMonitor ) {
        progressMonitor.beginTask(10, JavaMetadataI18n.sequencerTaskName);

        JavaMetadata javaMetadata = null;
        NameFactory nameFactory = context.getFactories().getNameFactory();
        PathFactory pathFactory = context.getFactories().getPathFactory();

        try {
            javaMetadata = JavaMetadata.instance(stream, JavaMetadataUtil.length(stream), null, progressMonitor.createSubtask(10));
            if (progressMonitor.isCancelled()) return;
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        if (javaMetadata != null) {
            Path javaCompilationUnitNode = pathFactory.create(JAVA_COMPILATION_UNIT_NODE);
            output.setProperty(javaCompilationUnitNode,
                               nameFactory.create(JAVA_COMPILATION_UNIT_PRIMARY_TYPE),
                               "java:compilationUnit");

            // Process package declaration of a unit.
            PackageMetadata packageMetadata = javaMetadata.getPackageMetadata();
            if (packageMetadata != null) {
                if (StringUtils.isNotEmpty(packageMetadata.getName())) {
                    
                    Path javaPackageDeclarationChildNode = pathFactory.create(JAVA_COMPILATION_UNIT_NODE + SLASH
                                                                              + JAVA_PACKAGE_CHILD_NODE + SLASH
                                                                              + JAVA_PACKAGE_DECLARATION_CHILD_NODE);
                    output.setProperty(javaPackageDeclarationChildNode,
                                       nameFactory.create(JAVA_PACKAGE_NAME),
                                       javaMetadata.getPackageMetadata().getName());
                }

                List<AnnotationMetadata> annotations = packageMetadata.getAnnotationMetada();
                if (!annotations.isEmpty()) {
                    for (AnnotationMetadata annotationMetadata : annotations) {
                        if (annotationMetadata instanceof MarkerAnnotationMetadata) {
                            MarkerAnnotationMetadata markerAnnotationMetadata = (MarkerAnnotationMetadata)annotationMetadata;
                            Path markerAnnotationChildNode = pathFactory.create(JAVA_COMPILATION_UNIT_NODE + SLASH
                                                                                + JAVA_PACKAGE_CHILD_NODE + SLASH
                                                                                + JAVA_PACKAGE_DECLARATION_CHILD_NODE + SLASH
                                                                                + JAVA_ANNOTATION_CHILD_NODE + SLASH
                                                                                + JAVA_ANNOTATION_DECLARATION_CHILD_NODE + SLASH
                                                                                + JAVA_ANNOTATION_TYPE_CHILD_NODE + SLASH
                                                                                + JAVA_MARKER_ANNOTATION_CHILD_NODE);
                            output.setProperty(markerAnnotationChildNode,
                                               nameFactory.create(JAVA_ANNOTATION_TYPE_NAME),
                                               markerAnnotationMetadata.getName());
                        }
                        if (annotationMetadata instanceof SingleMemberAnnotationMetadata) {
                            SingleMemberAnnotationMetadata singleMemberAnnotationMetadata = (SingleMemberAnnotationMetadata)annotationMetadata;
                            Path singleMemberAnnotationChildNode = pathFactory.create(JAVA_COMPILATION_UNIT_NODE + SLASH
                                                                                      + JAVA_PACKAGE_CHILD_NODE + SLASH
                                                                                      + JAVA_PACKAGE_DECLARATION_CHILD_NODE
                                                                                      + SLASH + JAVA_ANNOTATION_CHILD_NODE
                                                                                      + SLASH
                                                                                      + JAVA_ANNOTATION_DECLARATION_CHILD_NODE
                                                                                      + SLASH + JAVA_ANNOTATION_TYPE_CHILD_NODE
                                                                                      + SLASH
                                                                                      + JAVA_SINGLE_ELEMENT_ANNOTATION_CHILD_NODE);
                            output.setProperty(singleMemberAnnotationChildNode,
                                               nameFactory.create(JAVA_ANNOTATION_TYPE_NAME),
                                               singleMemberAnnotationMetadata.getName());
                        }
                        if (annotationMetadata instanceof NormalAnnotationMetadata) {
                            NormalAnnotationMetadata normalAnnotationMetadata = (NormalAnnotationMetadata)annotationMetadata;
                            Path normalAnnotationChildNode = pathFactory.create(JAVA_COMPILATION_UNIT_NODE + SLASH
                                                                                + JAVA_PACKAGE_CHILD_NODE + SLASH
                                                                                + JAVA_PACKAGE_DECLARATION_CHILD_NODE + SLASH
                                                                                + JAVA_ANNOTATION_CHILD_NODE + SLASH
                                                                                + JAVA_ANNOTATION_DECLARATION_CHILD_NODE + SLASH
                                                                                + JAVA_ANNOTATION_TYPE_CHILD_NODE + SLASH
                                                                                + JAVA_NORMAL_ANNOTATION_CHILD_NODE);
                            
                            
                            output.setProperty(normalAnnotationChildNode,
                                               nameFactory.create(JAVA_ANNOTATION_TYPE_NAME),
                                               normalAnnotationMetadata.getName());
                        }
                    }
                }
            }

            // Process import declarations of a unit
            //TODO write BDD to show how the works
        }
        progressMonitor.done();
    }
}
