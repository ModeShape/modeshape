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
package org.modeshape.test.integration.performance;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.modeshape.test.integration.performance.GeneratePerformanceRules.Visibility.PACKAGE;
import static org.modeshape.test.integration.performance.GeneratePerformanceRules.Visibility.PRIVATE;
import static org.modeshape.test.integration.performance.GeneratePerformanceRules.Visibility.PROTECTED;
import static org.modeshape.test.integration.performance.GeneratePerformanceRules.Visibility.PUBLIC;
import java.lang.reflect.Method;
import javax.jcr.Session;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.modeshape.jcr.JcrRepository;
import org.modeshape.jcr.Profiler;
import org.modeshape.test.integration.performance.GeneratePerformanceRules.Specification;

public class GeneratePerformanceRulesTest {

    private GeneratePerformanceRules generator;
    private Specification spec;

    @Before
    public void beforeEach() {
        generator = new GeneratePerformanceRules();
        spec = null;
    }

    @After
    public void afterEach() {
        generator = null;
        spec = null;
    }

    @Test
    public void shouldModifierComparisonForPublicMethod() throws Exception {
        spec = new Specification(JcrRepository.class, Session.class, PUBLIC);
        assertIncluded(Session.class, "getRootNode");
        assertIncluded(Session.class, "getNode", String.class);
    }

    @Test
    public void shouldModifierComparisonForPackageMethod() throws Exception {
        spec = new Specification(JcrRepository.class, Session.class, PACKAGE);
        assertExcluded(Session.class, "getRootNode");
    }

    @Test
    public void shouldModifierComparisonForProtectedMethod() throws Exception {
        spec = new Specification(JcrRepository.class, Session.class, PROTECTED);
        assertExcluded(Session.class, "getRootNode");
    }

    @Test
    public void shouldModifierComparisonForPrivateMethod() throws Exception {
        spec = new Specification(JcrRepository.class, Session.class, PRIVATE);
        assertExcluded(Session.class, "getRootNode");
    }

    @Test
    public void shouldGenerateCorrectSignatureForMethodWithNoParameters() throws Exception {
        spec = new Specification(JcrRepository.class, Session.class, PUBLIC);
        String signature = generator.signatureFor(Session.class.getMethod("getRootNode"));
        assertThat(signature, is("getRootNode()"));
    }

    @Test
    public void shouldGenerateCorrectSignatureForMethodWithOneParameter() throws Exception {
        spec = new Specification(JcrRepository.class, Session.class, PUBLIC);
        String signature = generator.signatureFor(Session.class.getMethod("getNode", String.class));
        assertThat(signature, is("getNode(java.lang.String)"));
    }

    @Ignore
    @Test
    public void shouldGenerateRulesForOneClass() throws Exception {
        generator.instrumentClass("org.modeshape.jcr.JcrSession(public:javax.jcr.Session)");
        generator.setOutputFile("System.out");
        generator.generateRules();
    }

    @Test
    public void shouldGenerateRulesForPerformance() throws Exception {
        generator.instrumentClass("org.modeshape.jcr.JcrSession(public:javax.jcr.Session)");
        generator.instrumentClass("org.modeshape.jcr.JcrWorkspace(public:javax.jcr.Workspace)");
        generator.setOutputFile("target/byteman/jcr-performance-generated.txt");
        generator.setHelperClass(Profiler.class.getCanonicalName());
        generator.generateRules();
    }

    protected void assertExcluded( Class<?> clazz,
                                   String methodName,
                                   Class<?>... paramTypes ) throws Exception {
        Method method = clazz.getMethod(methodName, paramTypes);
        assertThat(method, is(notNullValue()));
        assertThat(spec.includes(method), is(false));
    }

    protected void assertIncluded( Class<?> clazz,
                                   String methodName,
                                   Class<?>... paramTypes ) throws Exception {
        Method method = clazz.getMethod(methodName, paramTypes);
        assertThat(method, is(notNullValue()));
        assertThat(spec.includes(method), is(true));
    }
}
