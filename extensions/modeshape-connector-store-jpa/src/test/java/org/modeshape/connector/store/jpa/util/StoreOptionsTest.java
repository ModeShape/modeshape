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
package org.modeshape.connector.store.jpa.util;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Query;
import org.hibernate.ejb.Ejb3Configuration;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author Randall Hauch
 */
public class StoreOptionsTest {

    protected static EntityManagerFactory factory;
    protected static EntityManager manager;
    protected StoreOptions options;

    @BeforeClass
    public static void beforeAll() throws Exception {
        // Connect to the database ...
        Ejb3Configuration configurator = new Ejb3Configuration();
        configurator.addAnnotatedClass(StoreOptionEntity.class);
        configurator.setProperty("hibernate.dialect", "org.hibernate.dialect.HSQLDialect");
        configurator.setProperty("hibernate.connection.driver_class", "org.hsqldb.jdbcDriver");
        configurator.setProperty("hibernate.connection.username", "sa");
        configurator.setProperty("hibernate.connection.password", "");
        configurator.setProperty("hibernate.connection.url", "jdbc:hsqldb:.");
        configurator.setProperty("hibernate.show_sql", "false");
        configurator.setProperty("hibernate.format_sql", "true");
        configurator.setProperty("hibernate.use_sql_comments", "true");
        configurator.setProperty("hibernate.hbm2ddl.auto", "create");
        factory = configurator.buildEntityManagerFactory();
        manager = factory.createEntityManager();
    }

    @Before
    public void beforeEach() throws Exception {
        removeAllOptionEntities();
        options = new StoreOptions(manager);
    }

    @After
    public void afterEach() throws Exception {
        removeAllOptionEntities();
    }

    @AfterClass
    public static void afterAll() throws Exception {
        try {
            manager.close();
        } finally {
            factory.close();
        }
    }

    protected void removeAllOptionEntities() {
        try {
            manager.getTransaction().begin();
            List<StoreOptionEntity> optionEntities = getAllOptionEntities();
            for (StoreOptionEntity entity : optionEntities) {
                manager.remove(entity);
            }
            manager.getTransaction().commit();
        } catch (RuntimeException t) {
            manager.getTransaction().rollback();
            throw t;
        }
    }

    @SuppressWarnings( "unchecked" )
    protected List<StoreOptionEntity> getAllOptionEntities() {
        Query query = manager.createNamedQuery("StoreOptionEntity.findAll");
        return query.getResultList();
    }

    protected void assertNoOptions() {
        try {
            manager.getTransaction().begin();
            assertThat(getAllOptionEntities().isEmpty(), is(true));
            manager.getTransaction().commit();
        } catch (RuntimeException t) {
            manager.getTransaction().rollback();
            throw t;
        }
    }

    protected void assertOption( String name,
                                 String expectedValue ) {
        try {
            manager.getTransaction().begin();
            String actualValue = options.getOption(name);
            assertThat(expectedValue, is(actualValue));
            manager.getTransaction().commit();
        } catch (RuntimeException t) {
            manager.getTransaction().rollback();
            throw t;
        }
    }

    //
    // protected StoreOptionEntity getOption( String name ) {
    // return options.readStoreOption(name);
    // }
    //
    protected void setOptionInTxn( String name,
                                   String value ) {
        try {
            manager.getTransaction().begin();
            options.setOption(name, value);
            manager.getTransaction().commit();
        } catch (RuntimeException t) {
            manager.getTransaction().rollback();
            throw t;
        }
        assertOption(name, value);
    }

    @Test
    public void shouldReturnNullForNonExistantOption() {
        assertNoOptions();
        assertThat(options.getOption("non-existant name"), is(nullValue()));
    }

    @Test
    public void shouldReturnValueForExistingOption() {
        setOptionInTxn("name1", "value1");
        try {
            manager.getTransaction().begin();
            assertThat(options.getOption("name1"), is("value1"));
            manager.getTransaction().commit();
        } catch (RuntimeException t) {
            manager.getTransaction().rollback();
            throw t;
        }
    }

    @Test
    public void shouldSetValueOnExistingOption() {
        setOptionInTxn("name1", "value1");
        try {
            manager.getTransaction().begin();
            assertThat(options.getOption("name1"), is("value1"));
            options.setOption("name1", "value2");
            assertThat(options.getOption("name1"), is("value2"));
            manager.getTransaction().commit();
        } catch (RuntimeException t) {
            manager.getTransaction().rollback();
            throw t;
        }

        assertOption("name1", "value2");
    }

    @Test
    public void shouldRemoveOptionWhenSetToNullValue() {
        setOptionInTxn("name1", "value1");
        try {
            manager.getTransaction().begin();
            assertThat(options.getOption("name1"), is("value1"));
            options.setOption("name1", null);
            assertThat(options.getOption("name1"), is(nullValue()));
            manager.getTransaction().commit();
        } catch (RuntimeException t) {
            manager.getTransaction().rollback();
            throw t;
        }
    }

    @Test
    public void shouldNotRemoveOptionWhenSetToEmptyValue() {
        setOptionInTxn("name1", "value1");
        try {
            manager.getTransaction().begin();
            assertThat(options.getOption("name1"), is("value1"));
            options.setOption("name1", "");
            assertThat(options.getOption("name1"), is(""));
            manager.getTransaction().commit();
        } catch (RuntimeException t) {
            manager.getTransaction().rollback();
            throw t;
        }
    }
}
