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
package org.modeshape.web.jcr.rest;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import javax.ws.rs.core.Application;
import org.modeshape.web.jcr.rest.interceptor.CleanupInterceptor;
import org.modeshape.web.jcr.rest.interceptor.LoggingInterceptor;
import org.modeshape.web.jcr.rest.output.HtmlBodyWriter;
import org.modeshape.web.jcr.rest.output.JSONBodyWriter;
import org.modeshape.web.jcr.rest.output.TextBodyWriter;

/**
 * Implementation of the JAX-RS {@code Application} class to identify all JAX-RS providers and classes in the application.
 *
 * @see Application
 */
public final class JcrApplication extends Application {

    @SuppressWarnings( "deprecation" )
    @Override
    public Set<Class<?>> getClasses() {
        return new HashSet<Class<?>>(Arrays.asList(new Class<?>[] { JcrResources.class, ModeShapeRestService.class,
                HtmlBodyWriter.class, JSONBodyWriter.class, TextBodyWriter.class, LoggingInterceptor.class,
                CleanupInterceptor.class, ModeShapeExceptionMapper.class}));
    }
}
