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

package org.modeshape.common.component;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertThat;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;

/**
 * @author Randall Hauch
 * @author John Verhaeg
 */
public class ComponentLibraryTest {

    private ComponentLibrary<SampleComponent, SampleComponentConfig> library;
    private SampleComponentConfig configA;
    private SampleComponentConfig configB;
    private SampleComponentConfig configA2;
    private String validDescription;
    private String[] validClasspath;

    @Before
    public void beforeEach() {
        MockitoAnnotations.initMocks(this);
        this.library = new ComponentLibrary<SampleComponent, SampleComponentConfig>();
        this.validDescription = "a Component";
        this.validClasspath = new String[] {"com.acme:configA:1.0,com.acme:configB:1.0"};
        this.configA = new SampleComponentConfig("configA", validDescription, MockComponentA.class.getName(), validClasspath);
        this.configB = new SampleComponentConfig("configB", validDescription, MockComponentB.class.getName(), validClasspath);
        this.configA2 = new SampleComponentConfig("conFigA", validDescription, MockComponentA.class.getName(), validClasspath);
    }

    @Test
    public void shouldBeInstantiableWithDefaultConstructor() {
        new ComponentLibrary<SampleComponent, SampleComponentConfig>();
    }

    @Test
    public void shouldHaveDefaultClassLoaderFactory() {
        assertThat(this.library.getClassLoaderFactory(), is(ComponentLibrary.DEFAULT));
    }

    @Test
    public void shouldSetClassLoaderFactoryToDefaultIfSettingClassLoaderFactoryToNull() {
        library.setClassLoaderFactory(null);
        assertThat(this.library.getClassLoaderFactory(), is(ComponentLibrary.DEFAULT));
    }

    @Test
    public void shouldAddComponentWhenNoneExists() {
        assertThat(library.getInstances().size(), is(0));
        library.add(configA);
        assertThat(library.getInstances().size(), is(1));
    }

    @Test
    public void shouldAddComponentWhenMatchingConfigAlreadyExistsUnlessNotChanged() {
        assertThat(library.getInstances().size(), is(0));
        library.add(configA);
        assertThat(library.getInstances().size(), is(1));
        assertThat(library.getInstances().get(0).getConfiguration(), is(sameInstance(configA)));

        library.add(configA);
        assertThat(library.getInstances().size(), is(1));
        assertThat(library.getInstances().get(0).getConfiguration(), is(sameInstance(configA)));

        // Add another, but this isn't changed ...
        library.add(configA2);
        assertThat(library.getInstances().size(), is(1));
        assertThat(library.getInstances().get(0).getConfiguration(), is(sameInstance(configA)));

        // Change the configuration, then add another ...
        configA2 = new SampleComponentConfig("conFigA", "Config A v2", MockComponentA.class.getName(), validClasspath);
        library.add(configA2);
        assertThat(library.getInstances().size(), is(1));
        assertThat(library.getInstances().get(0).getConfiguration(), is(sameInstance(configA2)));

        // Add a second that isn't there
        library.add(configB);
        assertThat(library.getInstances().size(), is(2));
        assertThat(library.getInstances().get(1).getConfiguration(), is(sameInstance(configB)));
    }

    @Test
    public void shouldInstantiateAndConfigureComponentWhenConfigurationAddedOrUpdated() {
        assertThat(library.getInstances().size(), is(0));
        library.add(configA);
        List<SampleComponent> components = library.getInstances();
        assertThat(components.size(), is(1));
        MockComponentA firstComponent = (MockComponentA)components.get(0);
        assertThat(firstComponent.isConfigured(), is(true));

        // Update the configuration, and a new component should be instantiated ...
        library.add(configA);
        components = library.getInstances();
        assertThat(components.size(), is(1));
        assertThat(components.get(0), instanceOf(MockComponentA.class));
        MockComponentA secondComponentA = (MockComponentA)components.get(0);
        assertThat(secondComponentA.isConfigured(), is(true));

        // The current component should be the same instance since it was unchanged ...
        assertThat(secondComponentA, is(sameInstance(firstComponent)));

        // The current component should not be the same instance since it was just changed ...
        configA = new SampleComponentConfig("conFigA", "Config A v2", MockComponentA.class.getName(), validClasspath);
        library.add(configA);
        components = library.getInstances();
        assertThat(components.size(), is(1));
        assertThat(components.get(0), instanceOf(MockComponentA.class));
        secondComponentA = (MockComponentA)components.get(0);
        assertThat(secondComponentA.isConfigured(), is(true));
        assertThat(secondComponentA, is(not(sameInstance(firstComponent))));

        // Add a second component config, and the first component should not be changed
        library.add(configB);
        components = library.getInstances();
        assertThat(components.size(), is(2));
        assertThat(components.get(0), instanceOf(MockComponentA.class));
        assertThat(components.get(0), is(sameInstance((Component<SampleComponentConfig>)secondComponentA)));
        assertThat(components.get(1), instanceOf(MockComponentB.class));
        MockComponentB firstComponentB = (MockComponentB)components.get(1);

        // The very first component instance should still be runnable ...
        for (int i = 0; i != 10; ++i) {
            firstComponent.doSomething();
        }
        assertThat(firstComponent.getCounter(), is(10));

        // The second component instance should still be runnable ...
        for (int i = 0; i != 10; ++i) {
            secondComponentA.doSomething();
        }
        assertThat(secondComponentA.getCounter(), is(10));

        // The third component instance should still be runnable ...
        for (int i = 0; i != 10; ++i) {
            firstComponentB.doSomething();
        }
        assertThat(firstComponentB.getCounter(), is(10));
    }

    @Test
    public void shouldRefreshInstancesWhenCalledDirectly() {
        library.add(configA);
        library.add(configB);
        List<SampleComponent> components = library.getInstances();
        assertThat(components.size(), is(2));
        assertThat(components.get(0), instanceOf(MockComponentA.class));
        assertThat(components.get(1), instanceOf(MockComponentB.class));
        MockComponentA firstInstanceA = (MockComponentA)components.get(0);
        MockComponentB firstInstanceB = (MockComponentB)components.get(1);
        assertThat(components.get(0), is(sameInstance((SampleComponent)firstInstanceA)));
        assertThat(components.get(1), is(sameInstance((SampleComponent)firstInstanceB)));

        // Refresh the instances ...
        assertThat(library.refreshInstances(), is(true));

        // Check that there are instances for each of the components ...
        components = library.getInstances();
        assertThat(components.size(), is(2));
        assertThat(components.get(0), instanceOf(MockComponentA.class));
        assertThat(components.get(1), instanceOf(MockComponentB.class));
        MockComponentA secondInstanceA = (MockComponentA)components.get(0);
        MockComponentB secondInstanceB = (MockComponentB)components.get(1);
        assertThat(components.get(0), is(sameInstance((SampleComponent)secondInstanceA)));
        assertThat(components.get(1), is(sameInstance((SampleComponent)secondInstanceB)));

        // And check that the instances have changed ...
        assertThat(firstInstanceA, is(not(sameInstance((SampleComponent)secondInstanceA))));
        assertThat(firstInstanceB, is(not(sameInstance((SampleComponent)secondInstanceB))));
    }

    @Test
    public void shouldRefreshInstancesWhenSettingClassLoaderFactory() {
        library.add(configA);
        library.add(configB);
        List<SampleComponent> components = library.getInstances();
        assertThat(components.size(), is(2));
        assertThat(components.get(0), instanceOf(MockComponentA.class));
        assertThat(components.get(1), instanceOf(MockComponentB.class));
        MockComponentA firstInstanceA = (MockComponentA)components.get(0);
        MockComponentB firstInstanceB = (MockComponentB)components.get(1);
        assertThat(components.get(0), is(sameInstance((SampleComponent)firstInstanceA)));
        assertThat(components.get(1), is(sameInstance((SampleComponent)firstInstanceB)));

        // Set the class loader factory to the SAME instance, and this should always refresh the instances ...
        library.setClassLoaderFactory(library.getClassLoaderFactory());

        // Check that there are instances for each of the components ...
        components = library.getInstances();
        assertThat(components.size(), is(2));
        assertThat(components.get(0), instanceOf(MockComponentA.class));
        assertThat(components.get(1), instanceOf(MockComponentB.class));
        MockComponentA secondInstanceA = (MockComponentA)components.get(0);
        MockComponentB secondInstanceB = (MockComponentB)components.get(1);
        assertThat(components.get(0), is(sameInstance((SampleComponent)secondInstanceA)));
        assertThat(components.get(1), is(sameInstance((SampleComponent)secondInstanceB)));

        // And check that the instances have changed ...
        assertThat(firstInstanceA, is(not(sameInstance((SampleComponent)secondInstanceA))));
        assertThat(firstInstanceB, is(not(sameInstance((SampleComponent)secondInstanceB))));
    }
}
