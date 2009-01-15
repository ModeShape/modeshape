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
package org.jboss.dna.graph.request;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import java.util.UUID;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.Location;
import org.jboss.dna.graph.property.Name;
import org.jboss.dna.graph.property.Path;
import org.jboss.dna.graph.property.Property;
import org.jboss.dna.graph.request.Request;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Randall Hauch
 */
public abstract class AbstractRequestTest {

    protected ExecutionContext context;
    protected Location validPathLocation;
    protected Location validUuidLocation;
    protected Location validPropsLocation;
    protected Location validPathLocation1;
    protected Location validUuidLocation1;
    protected Location validPropsLocation1;
    protected Location validPathLocation2;
    protected Location validUuidLocation2;
    protected Location validPropsLocation2;
    protected Property validProperty1;
    protected Property validProperty2;

    @Before
    public void beforeEach() {
        context = new ExecutionContext();
        Path validPath = createPath("/a/b/c");
        UUID validUuid = UUID.randomUUID();
        Name idProperty1Name = createName("id1");
        Name idProperty2Name = createName("id2");
        Property idProperty1 = context.getPropertyFactory().create(idProperty1Name, "1");
        Property idProperty2 = context.getPropertyFactory().create(idProperty2Name, "2");
        validPathLocation = new Location(validPath);
        validUuidLocation = new Location(validUuid);
        validPropsLocation = new Location(idProperty1, idProperty2);

        validPathLocation1 = new Location(validPath);
        validUuidLocation1 = new Location(validUuid);
        validPropsLocation1 = new Location(idProperty1, idProperty2);

        validPath = createPath("/a/c/d");
        validUuid = UUID.randomUUID();
        idProperty1 = context.getPropertyFactory().create(idProperty1Name, "3");
        idProperty2 = context.getPropertyFactory().create(idProperty2Name, "4");
        validPathLocation2 = new Location(validPath);
        validUuidLocation2 = new Location(validUuid);
        validPropsLocation2 = new Location(idProperty1, idProperty2);

        validProperty1 = context.getPropertyFactory().create(createName("fooProperty"), "foo");
        validProperty2 = context.getPropertyFactory().create(createName("barProperty"), "bar");
    }

    protected Path createPath( String path ) {
        return context.getValueFactories().getPathFactory().create(path);
    }

    protected Name createName( String name ) {
        return context.getValueFactories().getNameFactory().create(name);
    }

    protected abstract Request createRequest();

    @Test
    public void shouldNotBeCancelledByDefault() {
        Request request = createRequest();
        assertThat(request.isCancelled(), is(false));
    }

    @Test
    public void shouldBeCancelledAfterCallingCancel() {
        Request request = createRequest();
        assertThat(request.isCancelled(), is(false));
        request.cancel();
        assertThat(request.isCancelled(), is(true));
    }
}
