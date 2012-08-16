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
package org.modeshape.web.jcr.rest;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import javax.ws.rs.core.Application;
import org.modeshape.web.jcr.rest.form.FileUploadForm;
import org.modeshape.web.jcr.rest.output.HtmlBodyWriter;
import org.modeshape.web.jcr.rest.output.JSONBodyWriter;
import org.modeshape.web.jcr.rest.output.TextBodyWriter;

/**
 * Implementation of the JAX-RS {@code Application} class to identify all JAX-RS providers and classes in the application.
 *
 * @see Application
 */
public class JcrApplication extends Application {

    /**
     * {@inheritDoc}
     *
     * @see Application#getClasses()
     */
    @Override
    public Set<Class<?>> getClasses() {
        return new HashSet<Class<?>>(Arrays.asList(new Class<?>[] {
                JcrResources.class,
                ModeShapeRestService.class,
                FileUploadForm.class,
                HtmlBodyWriter.class,
                JSONBodyWriter.class,
                TextBodyWriter.class,
                ExceptionMappers.PathNotFoundExceptionMapper.class,
                ExceptionMappers.JSONExceptionMapper.class,
                ExceptionMappers.NotFoundExceptionMapper.class,
                ExceptionMappers.NoSuchRepositoryExceptionMapper.class,
                ExceptionMappers.NoSuchWorkspaceExceptionMapper.class,
                ExceptionMappers.RepositoryExceptionMapper.class,
                ExceptionMappers.InvalidQueryExceptionMapper.class,
                ExceptionMappers.NoSuchNodeTypeExceptionMapper.class,
                ExceptionMappers.IllegalArgumentExceptionMapper.class
        }));
    }

}
