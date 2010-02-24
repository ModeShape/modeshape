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
package org.modeshape.connector.store.jpa;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;
import org.hibernate.ejb.Ejb3Configuration;
import org.modeshape.common.i18n.I18n;
import org.modeshape.graph.connector.RepositoryConnection;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.mockito.Mock;

/**
 * @author Randall Hauch
 */
public class ModelTest {

    private String validName;
    private I18n validDescription;
    private I18n validDescription2;
    private Model model1;
    private Model model2;
    private Model model3;
    @Mock
    private RepositoryConnection connection;

    @Before
    public void beforeEach() throws Exception {
        MockitoAnnotations.initMocks(this);
        validName = "Concrete Model";
        validDescription = JpaConnectorI18n.basicModelDescription;
        validDescription2 = JpaConnectorI18n.connectorName;
        model1 = new ConcreteModel(validName, validDescription);
        model2 = new ConcreteModel(validName, validDescription2);
        model3 = new ConcreteModel(validName + " ", validDescription);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowNullNameInConstructor() {
        new ConcreteModel(null, validDescription);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowEmptyNameInConstructor() {
        new ConcreteModel("", validDescription);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowNullDescriptionInConstructor() {
        new ConcreteModel(validName, null);
    }

    @Test
    public void shouldNotTrimName() {
        assertThat(model3.getName(), is(validName + " "));
    }

    @Test
    public void shouldConsiderTwoModelsEqualIfTheyHaveTheSameName() {
        assertThat(model1, is(model2));
    }

    @Test
    public void shouldConsiderTwoModelsNotEqualIfTheyHaveDifferentNames() {
        assertThat(model1, is(not(model3)));
        assertThat(model2, is(not(model3)));
    }

    protected class ConcreteModel extends Model {
        protected ConcreteModel( String name,
                                 I18n description ) {
            super(name, description);
        }

        @Override
        public void configure( Ejb3Configuration configurator ) {
        }

        @SuppressWarnings( "synthetic-access" )
        @Override
        public RepositoryConnection createConnection( JpaSource source ) {
            return connection;
        }

    }
}
