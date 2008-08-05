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
package org.jboss.dna.spi.graph.commands.executor;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.stub;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.jboss.dna.spi.ExecutionContext;
import org.jboss.dna.spi.graph.commands.CompositeCommand;
import org.jboss.dna.spi.graph.commands.CopyBranchCommand;
import org.jboss.dna.spi.graph.commands.CopyNodeCommand;
import org.jboss.dna.spi.graph.commands.CreateNodeCommand;
import org.jboss.dna.spi.graph.commands.DeleteBranchCommand;
import org.jboss.dna.spi.graph.commands.GetChildrenCommand;
import org.jboss.dna.spi.graph.commands.GetNodeCommand;
import org.jboss.dna.spi.graph.commands.GetPropertiesCommand;
import org.jboss.dna.spi.graph.commands.GraphCommand;
import org.jboss.dna.spi.graph.commands.MoveBranchCommand;
import org.jboss.dna.spi.graph.commands.RecordBranchCommand;
import org.jboss.dna.spi.graph.commands.SetPropertiesCommand;
import org.jboss.dna.spi.graph.connection.BasicExecutionContext;
import org.jboss.dna.spi.graph.connection.RepositorySourceException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.mockito.MockitoAnnotations.Mock;

/**
 * @author Randall Hauch
 */
public class AbstractCommandExecutorTest {

    private static final List<GraphCommand> EMPTY_COMMAND_LIST = Collections.emptyList();
    private static final List<GraphCommand> NULL_COMMAND_LIST = Collections.singletonList(null);

    private AbstractCommandExecutor executor;
    private GraphCommand command;
    private ExecutionContext context;
    @Mock
    protected CommandExecutor validator;

    @Before
    public void beforeEach() throws Exception {
        MockitoAnnotations.initMocks(this);
        context = new BasicExecutionContext();
        executor = new ExecutorImpl(context, "Source X", validator);
    }

    @Test
    public void shouldHaveEnvironment() {
        assertThat(executor.getExecutionContext(), is(sameInstance(context)));
    }

    @Test
    public void shouldIgnoreNullCommands() throws Exception {
        executor.execute(command);
        verify(validator, times(1)).execute(command);
        verifyNoMoreInteractions(validator);
    }

    @Test( expected = AssertionError.class )
    public void shouldNotAllowNullCompositeCommand() throws Exception {
        executor.execute((CompositeCommand)null);
    }

    @Test
    public void shouldCorrectlyExecuteEmptyCompositeCommandByCallingNoOtherExecuteMethods() throws Exception {
        CompositeCommand command = mock(CompositeCommand.class);
        stub(command.iterator()).toReturn(EMPTY_COMMAND_LIST.iterator());
        executor.execute(command);
        verify(validator, times(1)).execute(command);
        verifyNoMoreInteractions(validator);
    }

    @Test
    public void shouldCorrectlyExecuteCompositeCommandWithSingleNullValueByCallingNoOtherExecuteMethods() throws Exception {
        CompositeCommand command = mock(CompositeCommand.class);
        stub(command.iterator()).toReturn(NULL_COMMAND_LIST.iterator());
        executor.execute(command);
        verify(validator, times(1)).execute((GraphCommand)null);
        verify(validator, times(1)).execute(command);
        verifyNoMoreInteractions(validator);
    }

    @Test
    public void shouldCorrectlyExecuteCompositeCommandWithNullValueByCallingNoOtherExecuteMethods() throws Exception {
        CreateNodeCommand createNodeCommand = mock(CreateNodeCommand.class);
        GetNodeCommand getNodeCommand = mock(GetNodeCommand.class);
        CompositeCommand command = mock(CompositeCommand.class);
        stub(command.iterator()).toReturn(Arrays.asList(new GraphCommand[] {createNodeCommand, getNodeCommand, null,
            getNodeCommand}).iterator());
        executor.execute(command);
        verify(validator, times(1)).execute(command);
        verify(validator, times(1)).execute((GraphCommand)null);
        verify(validator, times(1)).execute((GraphCommand)createNodeCommand);
        verify(validator, times(2)).execute((GraphCommand)getNodeCommand);
        verify(validator, times(2)).execute((GetPropertiesCommand)getNodeCommand);
        verify(validator, times(2)).execute((GetChildrenCommand)getNodeCommand);
        verify(validator, times(1)).execute(createNodeCommand);
        verify(validator, times(2)).execute(getNodeCommand);
        verifyNoMoreInteractions(validator);
    }

    @Test
    public void shouldCorrectlyDelegateCompositeCommands() throws Exception {
        CreateNodeCommand createNodeCommand = mock(CreateNodeCommand.class);
        GetNodeCommand getNodeCommand = mock(GetNodeCommand.class);
        CompositeCommand command = mock(CompositeCommand.class);
        stub(command.iterator()).toReturn(Arrays.asList(new GraphCommand[] {createNodeCommand, getNodeCommand, getNodeCommand}).iterator());
        executor.execute(command);
        verify(validator, times(1)).execute(command);
        verify(validator, times(1)).execute((GraphCommand)createNodeCommand);
        verify(validator, times(2)).execute((GraphCommand)getNodeCommand);
        verify(validator, times(2)).execute((GetPropertiesCommand)getNodeCommand);
        verify(validator, times(2)).execute((GetChildrenCommand)getNodeCommand);
        verify(validator, times(1)).execute(createNodeCommand);
        verify(validator, times(2)).execute(getNodeCommand);
        verifyNoMoreInteractions(validator);
    }

    @Test
    public void shouldExecuteGetPropertiesAndGetChildrenCommandsForGetNodeCommand() throws Exception {
        GetNodeCommand getNodeCommand = mock(GetNodeCommand.class);
        executor.execute((GraphCommand)getNodeCommand);
        verify(validator, times(1)).execute((GraphCommand)getNodeCommand);
        verify(validator, times(1)).execute((GetPropertiesCommand)getNodeCommand);
        verify(validator, times(1)).execute((GetChildrenCommand)getNodeCommand);
        verify(validator, times(1)).execute(getNodeCommand);
        verifyNoMoreInteractions(validator);
    }

    @Test
    public void shouldExecuteEitherCopyBranchOrCopyNodeCommandButNotBoth() throws Exception {
        CopyBranchCommand copyBranchCommand = mock(CopyBranchCommand.class);
        CopyNodeCommand copyNodeCommand = mock(CopyNodeCommand.class);
        executor.execute((GraphCommand)copyBranchCommand);
        verify(validator, times(1)).execute((GraphCommand)copyBranchCommand);
        verify(validator, times(1)).execute(copyBranchCommand);
        verify(validator, times(0)).execute(copyNodeCommand);
        executor.execute((GraphCommand)copyNodeCommand);
        verify(validator, times(1)).execute((GraphCommand)copyNodeCommand);
        verify(validator, times(1)).execute(copyBranchCommand);
        verify(validator, times(1)).execute(copyNodeCommand);
        verifyNoMoreInteractions(validator);
    }

    @Test
    public void shouldExecuteEitherCreateNodeOrSetPropertiesCommandButNotBoth() throws Exception {
        CreateNodeCommand createNodeCommand = mock(CreateNodeCommand.class);
        SetPropertiesCommand setPropertiesCommand = mock(SetPropertiesCommand.class);
        executor.execute((GraphCommand)createNodeCommand);
        verify(validator, times(1)).execute((GraphCommand)createNodeCommand);
        verify(validator, times(1)).execute(createNodeCommand);
        verify(validator, times(0)).execute(setPropertiesCommand);
        executor.execute((GraphCommand)setPropertiesCommand);
        verify(validator, times(1)).execute((GraphCommand)setPropertiesCommand);
        verify(validator, times(1)).execute(createNodeCommand);
        verify(validator, times(1)).execute(setPropertiesCommand);
        verifyNoMoreInteractions(validator);
    }

    @Test
    public void shouldExecuteEitherDeleteOrMoveBranchComamndButNotBoth() throws Exception {
        DeleteBranchCommand deleteBranchCommand = mock(DeleteBranchCommand.class);
        MoveBranchCommand moveBranchCommand = mock(MoveBranchCommand.class);
        executor.execute((GraphCommand)deleteBranchCommand);
        verify(validator, times(1)).execute((GraphCommand)deleteBranchCommand);
        verify(validator, times(1)).execute(deleteBranchCommand);
        verify(validator, times(0)).execute(moveBranchCommand);
        executor.execute((GraphCommand)moveBranchCommand);
        verify(validator, times(1)).execute((GraphCommand)moveBranchCommand);
        verify(validator, times(1)).execute(deleteBranchCommand);
        verify(validator, times(1)).execute(moveBranchCommand);
        verifyNoMoreInteractions(validator);
    }

    @Test
    public void shouldExecuteRecordBranchCommand() throws Exception {
        RecordBranchCommand recordBranchCommand = mock(RecordBranchCommand.class);
        executor.execute((GraphCommand)recordBranchCommand);
        verify(validator, times(1)).execute((GraphCommand)recordBranchCommand);
        verify(validator, times(1)).execute(recordBranchCommand);
        verifyNoMoreInteractions(validator);
    }

    protected static class ExecutorImpl extends AbstractCommandExecutor {

        private final CommandExecutor validator;

        protected ExecutorImpl( ExecutionContext context,
                                String name,
                                CommandExecutor validator ) {
            super(context, name);
            this.validator = validator;
        }

        @Override
        public void execute( GraphCommand command ) throws RepositorySourceException, InterruptedException {
            super.execute(command);
            validator.execute(command);
        }

        @Override
        public void execute( CompositeCommand command ) throws RepositorySourceException, InterruptedException {
            super.execute(command);
            validator.execute(command);
        }

        @Override
        public void execute( CopyBranchCommand command ) throws RepositorySourceException, InterruptedException {
            validator.execute(command);
        }

        @Override
        public void execute( CopyNodeCommand command ) throws RepositorySourceException, InterruptedException {
            validator.execute(command);
        }

        @Override
        public void execute( CreateNodeCommand command ) throws RepositorySourceException, InterruptedException {
            validator.execute(command);
        }

        @Override
        public void execute( DeleteBranchCommand command ) throws RepositorySourceException, InterruptedException {
            validator.execute(command);
        }

        @Override
        public void execute( GetChildrenCommand command ) throws RepositorySourceException, InterruptedException {
            validator.execute(command);
        }

        @Override
        public void execute( GetPropertiesCommand command ) throws RepositorySourceException, InterruptedException {
            validator.execute(command);
        }

        @Override
        public void execute( MoveBranchCommand command ) throws RepositorySourceException, InterruptedException {
            validator.execute(command);
        }

        @Override
        public void execute( GetNodeCommand command ) throws RepositorySourceException, InterruptedException {
            validator.execute(command);
        }

        @Override
        public void execute( RecordBranchCommand command ) throws RepositorySourceException, InterruptedException {
            validator.execute(command);
        }

        @Override
        public void execute( SetPropertiesCommand command ) throws RepositorySourceException, InterruptedException {
            validator.execute(command);
        }
    }

}
