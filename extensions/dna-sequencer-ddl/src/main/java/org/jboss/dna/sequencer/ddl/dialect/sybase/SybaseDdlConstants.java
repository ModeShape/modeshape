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
package org.jboss.dna.sequencer.ddl.dialect.sybase;

import java.util.Arrays;
import java.util.List;

import org.jboss.dna.sequencer.ddl.DdlConstants;

/**
 * @author blafond
 *
 */
public interface SybaseDdlConstants extends DdlConstants {
	public static final String[] CUSTOM_KEYWORDS = {
		
	};

	interface SybaseStatementStartPhrases {
	    static final String[][] ALTER_PHRASES = { 
		};
	    
	    static final String[][] CREATE_PHRASES = { 
	    	
		};
	    static final
	    String[][] DROP_PHRASES = { 
		};
	    static final String[][] SET_PHRASES = { 
	
		};	    
	    static final String[][] MISC_PHRASES = { 
		};
	}
	
	interface SybaseDataTypes {
		static final List<String[]> CUSTOM_DATATYPE_START_PHRASES = 
	  		Arrays.asList(new String[][] {
	  	  	});
		
	  	static final List<String> CUSTOM_DATATYPE_START_WORDS = 
	  		Arrays.asList(new String[] {
	  	  	});
	}
}
