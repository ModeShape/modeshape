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
grammar Cnd;

options {//	backtrack=true;
//			memoize=true;
			output=AST;
		}

tokens {
	COMMA = ',';
	DASH = '-';
	DOT = '.';
	EQUALS = '=';
	EXCLAMATION = '!';
	GREATER_THAN = '>';
	LEFT_BRACKET = '[';
	LEFT_PAREN = '(';
	LESS_THAN = '<';
	PLUS = '+';
	RIGHT_BRACKET = ']';
	RIGHT_PAREN = ')';
	SINGLE_QUOTE = '\'';
	SLASH = '/';
	STAR = '*';
	
	ATTRIBUTES;
	COMMENTS;
	DEFAULT_TYPE;
	DEFAULT_VALUES;
	JCR_NAME;
	NAME;
	NAMESPACES;
	CHILD_NODE_DEFINITION;
	CHILD_NODE_DEFINITIONS;
	NODE_TYPE_DEFINITION;
	NODE_TYPE_DEFINITIONS;
	NODE_TYPE_OPTIONS;
	PROPERTIES;
	PROPERTY_DEFINITION;
	PROPERTY_TYPE;
	REQUIRED_TYPES;
	SUPERTYPES;
	VALUE_CONSTRAINTS;
}

@header {
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
package org.jboss.dna.sequencer.cnd;
}

@lexer::header {
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
package org.jboss.dna.sequencer.cnd;
}

// Make the parser exit immediately upon recognition error by overriding the following two methods,
// and by altering how the parser responds to thrown exceptions.
@members {
    /**
     * Make the parser exit immediately upon a recognition error by throwing an exception after a mismatch.
     */
    @Override
    protected void mismatch( IntStream input, int ttype, BitSet follow ) throws RecognitionException {
        throw new MismatchedTokenException(ttype,input);
    }
    
    /**
     * Make the parser exit immediately upon a recognition error by not recovering from a mismatched set.
     */
    @Override
    public void recoverFromMismatchedSet( IntStream input, RecognitionException e, BitSet follow ) throws RecognitionException {
        throw e;
    }
}

/*-----------------------------------------------------------------------------
 * PARSER RULES
 *---------------------------------------------------------------------------*/

cnd
	:	(	comment
			| ns_mapping
			| node_type_def
		)*
		
	->	^(COMMENTS comment*)?
		^(NAMESPACES ns_mapping*)?
		^(NODE_TYPE_DEFINITIONS node_type_def*)?
	;

comment
	: 	SINGLE_LINE_COMMENT
		| MULTI_LINE_COMMENT
	;

ns_mapping
	: NAMESPACE_MAPPING
	;

node_type_def
	: 	(	node_type_name
			supertypes?
			node_type_options?
			(	property_def
				| child_node_def
			)*
		)
		
	->	^(	NODE_TYPE_DEFINITION 
			^(JCR_NAME node_type_name)
			^(SUPERTYPES supertypes)?
			^(NODE_TYPE_OPTIONS node_type_options)?
			^(PROPERTIES property_def*)?
			^(CHILD_NODE_DEFINITIONS child_node_def*)?
		)
	;

node_type_name
	: 	(	LEFT_BRACKET
			string
			RIGHT_BRACKET
		)
		
	-> string
	;

supertypes
	:	(	GREATER_THAN
			string_list
		)
		
	-> string_list
	;

fragment
node_type_options
	:	(abs_opt ord_mix_opt?)
		| (ord_mix_opt abs_opt?)
	;

fragment
abs_opt
	:	'abstract'
		| 'abs'
		| 'a'
	;

fragment
ord_mix_opt 
	:	(orderable_opt mixin_opt?)
		| (mixin_opt orderable_opt?)
	;

fragment
orderable_opt
	:	'orderable'
		| 'ord'
		| 'o'
	;

fragment
mixin_opt
	:	'mixin'
		| 'mix'
		| 'm'
	;


property_def
	: 	(	DASH
			jcr_name
			property_type_decl?
			default_values?
			attributes*
			value_constraints?
		)
		
	->	^(	PROPERTY_DEFINITION
			^(JCR_NAME jcr_name)
			^(PROPERTY_TYPE property_type_decl)?
			^(DEFAULT_VALUES default_values)?
			^(ATTRIBUTES attributes*)?
			^(VALUE_CONSTRAINTS value_constraints)?
		)
	;

child_node_def
	:	PLUS
		jcr_name
		required_types?
		default_type?
		attributes?

	->	^(	CHILD_NODE_DEFINITION
			^(JCR_NAME jcr_name)
			^(REQUIRED_TYPES required_types)?
			^(DEFAULT_TYPE default_type)?
			^(ATTRIBUTES attributes*)?
		)
	;

fragment
jcr_name
	: 	string
		| STAR
	;

fragment
property_type_decl
	: 	LEFT_PAREN
		property_type
		RIGHT_PAREN
	
	-> property_type
	;

fragment
property_type
	: 	'BINARY' | 'Binary' | 'binary'
		| 'BOOLEAN' | 'Boolean' | 'boolean'
		| 'DATE' | 'Date' | 'date'
		| 'DECIMAL' | 'Decimal' | 'decimal'
		| 'DOUBLE' | 'Double' | 'double'
		| 'LONG' | 'Long' | 'long'
		| 'NAME' | 'Name' | 'name'
		| 'PATH' | 'Path' | 'path'
		| 'REFERENCE' | 'Reference' | 'reference'
		| 'STRING' | 'String' | 'string'
		| 'UNDEFINED' | 'Undefined' | 'undefined'
		| 'URI' | 'Uri' | 'uri'
		| 'WEAKREFERENCE' | 'WeakReference' | 'weakreference'
		| STAR
	;

fragment
default_values
	:	EQUALS
		string_list
		
	->	string_list
	;

fragment
value_constraints
	:	LESS_THAN
		string_list

	-> string_list
	;

fragment
required_types
	:	LEFT_PAREN
		string_list
		RIGHT_PAREN
		
	-> string_list
	;

fragment
default_type
	: 	EQUALS
		string
		
	-> string
	;

fragment
attributes
	:	'ABORT' | 'Abort' | 'abort'
		| 'autocreated' | 'aut' | 'a'
		| 'COMPUTE' | 'Compute' | 'compute'
		| 'COPY' | 'Copy' | 'copy'
		| 'IGNORE' | 'Ignore' | 'ignore'
		| 'INITIALIZE' | 'Initialize' | 'initialize'
		| 'mandatory' | 'man' | 'm'
		| 'multiple' | 'mul' | STAR
		| 'primary' | 'pri' | EXCLAMATION
		| 'protected' | 'pro' | 'p'
		| 'VERSION' | 'Version' | 'version'
	;

fragment
string
	:	QUOTED_STRING
		| UNQUOTED_STRING
	;

fragment
string_list
	: 	string
		(	COMMA
			string
		)*
		
	-> string+
	;

fragment
uri_string 
	:	QUOTED_URI_STRING
		| UNQUOTED_URI_STRING
	;

/*-----------------------------------------------------------------------------
 * LEXER RULES
 *---------------------------------------------------------------------------*/

MULTI_LINE_COMMENT
    :   (	'/*'
    		( options {greedy=false;} : . )*
    		'*/'
    	)
    ;

NAMESPACE_MAPPING
	: 	(	LESS_THAN
			(QUOTED_STRING | UNQUOTED_STRING)
			EQUALS
			(QUOTED_URI_STRING | UNQUOTED_URI_STRING)
			GREATER_THAN
		)
	;

QUOTED_STRING
	:	(	SINGLE_QUOTE
			UNQUOTED_STRING
			SINGLE_QUOTE
		)
	;


QUOTED_URI_STRING
	:	(	SINGLE_QUOTE
			UNQUOTED_URI_STRING
			SINGLE_QUOTE
		)
	;

SINGLE_LINE_COMMENT
    : 	'//'
    	~('\n' | '\r')*
    ;


UNQUOTED_STRING
	: 	(	'a' .. 'z'
			| 'A' .. 'Z'
			| '0' .. '9'
			| ':'
			| '_'
		)+
	;

fragment
URI_SPECIAL_CHARS
	:	SLASH
		| DOT
	;


UNQUOTED_URI_STRING
	: 	(	UNQUOTED_STRING
			| URI_SPECIAL_CHARS
		)+
	;

WS
	:	(	' '
			| '\r'
			|'\t'
			|'\u000C'
			|'\n'
		)+
		{$channel=HIDDEN;}
	;
