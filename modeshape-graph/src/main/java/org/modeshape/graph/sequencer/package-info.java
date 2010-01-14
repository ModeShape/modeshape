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
/**
 * A sequencer in ModeShape is a component that is able to process information (usually the content of a file,
 * or a property value on a node) and recreate that information as a graph of structured content.  This package
 * defines the interfaces for the sequencing system.
 * <h3>StreamSequencer</h3>
 * <p>The {@link StreamSequencer} interface is a special form of sequencer that processes information coming
 * through an {@link java.io.InputStream}. Implementations are responsible for processing the content and generating
 * structured content using the supplied {@link SequencerOutput} interface.  Additional details about the information
 * being sequenced is available in the supplied {@link StreamSequencerContext}.
 * </p>
 */

package org.modeshape.graph.sequencer;

