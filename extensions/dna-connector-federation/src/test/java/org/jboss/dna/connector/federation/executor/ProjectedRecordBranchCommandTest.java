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
package org.jboss.dna.connector.federation.executor;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.stub;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import org.jboss.dna.graph.commands.RecordBranchCommand;
import org.jboss.dna.graph.properties.Path;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.mockito.MockitoAnnotations.Mock;

/**
 * @author Randall Hauch
 */
public class ProjectedRecordBranchCommandTest {
    private ProjectedRecordBranchCommand command;
    @Mock
    private RecordBranchCommand wrapped;
    @Mock
    private Path projectedPath;

    @Before
    public void beforeEach() throws Exception {
        MockitoAnnotations.initMocks(this);
        command = new ProjectedRecordBranchCommand(wrapped, projectedPath);
    }

    @Test
    public void shouldReturnProjectedPath() {
        assertThat(command.getPath(), is(sameInstance(projectedPath)));
        verifyZeroInteractions(wrapped);
    }

    @Test
    public void shouldReturnErrorFromWrappedCommands() {
        Throwable error = mock(Throwable.class);
        stub(wrapped.getError()).toReturn(error);
        assertThat(command.getError(), is(error));
        verify(wrapped).getError();
    }

    @Test
    public void shouldCheckWrappedCommandForError() {
        assertThat(command.hasError(), is(false));
        verify(wrapped).hasError();
    }

    @Test
    public void shouldCheckWrappedCommandForNoError() {
        stub(wrapped.hasNoError()).toReturn(true);
        assertThat(command.hasNoError(), is(true));
        verify(wrapped).hasNoError();
    }

    @Test
    public void shouldSetErrorOnWrappedCommand() {
        Throwable error = mock(Throwable.class);
        command.setError(error);
        verify(wrapped).setError(error);
    }

    @Test
    public void shouldCheckWrappedCommandForCancellation() {
        stub(wrapped.isCancelled()).toReturn(true);
        assertThat(command.isCancelled(), is(true));
        verify(wrapped).isCancelled();
    }

    @Test
    public void shouldReturnWrappedCommandAsOriginalCommand() {
        assertThat(command.getOriginalCommand(), is(sameInstance(wrapped)));
    }

}
