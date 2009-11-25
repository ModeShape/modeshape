/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * JBoss DNA is free software. Unless otherwise indicated, all code in JBoss DNA
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * JBoss DNA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.sequencer.ddl;

import org.jboss.dna.common.text.ParsingException;
import org.jboss.dna.common.text.Position;

/**
 *
 */
public class DdlParserProblem extends ParsingException implements DdlConstants.Problems {
	/**
	 * 
	 */
	private static final long serialVersionUID = 2010539270968770893L;
	
	private int level = OK;

	private String unusedSource;
	
	/**
	 * @param position
	 */
	public DdlParserProblem(Position position) {
		super(position);

	}

	/**
	 * @param level 
	 * @param position
	 * @param message
	 * @param cause
	 */
	public DdlParserProblem(int level, Position position, String message, Throwable cause) {
		super(position, message, cause);
		this.level = level;
	}

	/**
	 * @param level 
	 * @param position
	 * @param message
	 */
	public DdlParserProblem(int level, Position position, String message) {
		super(position, message);
		this.level = level;
	}
	
	/**
	 * 
	 * 
	 * @return the unused statement string
	 */
	public String getUnusedSource() {
		return this.unusedSource;
	}
	
	/**
	 * @param unusedSource 
	 * 
	 */
	public void setUnusedSource(String unusedSource) {
		if( unusedSource == null ) {
			unusedSource = "";
		}
		this.unusedSource = unusedSource;
	}


	public void appendSource( boolean addSpaceBefore, String value) {
		if( addSpaceBefore ) {
			this.unusedSource = this.unusedSource + DdlConstants.SPACE;
		}
		this.unusedSource = this.unusedSource + value;
	}
	

	public void appendSource( boolean addSpaceBefore, String value, String... additionalStrs) {
		if( addSpaceBefore ) {
			this.unusedSource = this.unusedSource + DdlConstants.SPACE;
		}
		this.unusedSource = this.unusedSource + value;
	}
	
	/**
	 * @return level
	 */
	public int getLevel() {
		return level;
	}

	/**
	 * @param level Sets level to the specified value.
	 */
	public void setLevel(int level) {
		this.level = level;
	}
	
	/**
	 * {@inheritDoc}
	 *
	 * @see java.lang.Throwable#toString()
	 */
	@Override
	public String toString() {
		if( this.level == WARNING ) {
			return ("WARNING: " + super.toString());
		} else if( this.level == ERROR ) {
			return ("ERROR: " + super.toString());
		}
		
		return super.toString();
	}
	
}
