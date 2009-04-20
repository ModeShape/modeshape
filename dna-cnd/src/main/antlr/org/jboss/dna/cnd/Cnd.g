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
grammar Cnd;

options {
	output=AST;
}


tokens {
	NAMESPACES;
	PREFIX;
	URI;
	NODE;
	NAME;
	PRIMARY_TYPE;
	SUPERTYPES;
	NODE_TYPES;
	NODE_TYPE_ATTRIBUTES;
	HAS_ORDERABLE_CHILD_NODES;
	IS_MIXIN;
	IS_ABSTRACT;
	IS_QUERYABLE;
	PRIMARY_ITEM_NAME;
	PROPERTY_DEFINITION;
	REQUIRED_TYPE;
	DEFAULT_VALUES;
	VALUE_CONSTRAINTS;
	AUTO_CREATED;
	MANDATORY;
	PROTECTED;
	REQUIRED_TYPE;
	ON_PARENT_VERSION;
	MULTIPLE;
	QUERY_OPERATORS;
	IS_FULL_TEXT_SEARCHABLE;
	IS_QUERY_ORDERERABLE;
	CHILD_NODE_DEFINITION;
	REQUIRED_PRIMARY_TYPES;
	DEFAULT_PRIMARY_TYPE;
	SAME_NAME_SIBLINGS;
}

@header {
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
package org.jboss.dna.cnd;
}

@members {
@Override
public void emitErrorMessage( String msg ) {
    // don't write messages to System.err ...
    //super.emitErrorMessage(msg);
}
}

@lexer::header {
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
package org.jboss.dna.cnd;
}

@rulecatch {
  catch (RecognitionException re) {
    reportError(re);
  }
}

// ------------
// Parser rules
// ------------

// These rules generate an AST that was designed with a structure that may be 
// automatically processed into a graph structure.  This structure is:
//
//   NAMESPACES
//    +- NODE (multiple)
//        +- PREFIX
//            +- string value
//        +- URI
//            +- string value
//   NODE_TYPES
//    +- NODE (multiple)
//        +- NAME                                 [nt:nodeType/@jcr:name]
//            +- string value
//        +- PRIMARY_TYPE                         [nt:base/@jcr:primaryType]
//            +- string with value 'nt:nodeType'
//        +- SUPERTYPES                           [nt:nodeType/@jcr:supertypes]
//            +- string value(s)
//        +- IS_ABSTRACT                          [nt:nodeType/@jcr:isAbstract]
//            +- string containing boolean value (or false if not present)
//        +- HAS_ORDERABLE_CHILD_NODES            [nt:nodeType/@jcr:hasOrderableChildNodes]
//            +- string containing boolean value (or false if not present)
//        +- IS_MIXIN                             [nt:nodeType/@jcr:isMixin]
//            +- string containing boolean value (or false if not present)
//        +- IS_QUERYABLE                         [nt:nodeType/@jcr:isQueryable]
//            +- string containing boolean value (or true if not present)
//        +- PRIMARY_ITEM_NAME                    [nt:nodeType/@jcr:primaryItemName]
//            +- string containing string value
//        +- PROPERTY_DEFINITION                  [nt:nodeType/@jcr:propertyDefinition]
//            +- NODE (multiple)
//                +- NAME                         [nt:propertyDefinition/@jcr:name]
//                    +- string value
//                +- PRIMARY_TYPE                 [nt:base/@jcr:primaryType]
//                    +- string with value 'nt:propertyDefinition'
//                +- REQUIRED_TYPE                [nt:propertyDefinition/@jcr:propertyType]
//                    +- string value (limited to one of the predefined types)
//                +- DEFAULT_VALUES               [nt:propertyDefinition/@jcr:defaultValues]
//                    +- string value(s)
//                +- MULTIPLE                     [nt:propertyDefinition/@jcr:multiple]
//                    +- string containing boolean value (or false if not present)
//                +- MANDATORY                    [nt:propertyDefinition/@jcr:mandatory]
//                    +- string containing boolean value (or false if not present)
//                +- AUTO_CREATED                 [nt:propertyDefinition/@jcr:autoCreated]
//                    +- string containing boolean value (or false if not present)
//                +- PROTECTED                    [nt:propertyDefinition/@jcr:protected]
//                    +- string containing boolean value (or false if not present)
//                +- ON_PARENT_VERSION            [nt:propertyDefinition/@jcr:onParentVersion]
//                    +- string value (limited to one of the predefined literal values)
//                +- QUERY_OPERATORS              
//                    +- string value (containing a comma-separated list of operator literals)
//                +- IS_FULL_TEXT_SEARCHABLE      [nt:propertyDefinition/@jcr:isFullTextSearchable]
//                    +- string containing boolean value (or true if not present)
//                +- IS_QUERY_ORDERABLE           [nt:propertyDefinition/@jcr:isQueryOrderable]
//                    +- string containing boolean value (or true if not present)
//                +- VALUE_CONSTRAINTS            [nt:propertyDefinition/@jcr:valueConstraints]
//                    +- string value(s)
//        +- CHILD_NODE_DEFINITION                [nt:nodeType/@jcr:childNodeDefinition]
//            +- NODE (multiple)
//                +- NAME                         [nt:childNodeDefinition/@jcr:name]
//                    +- string value
//                +- PRIMARY_TYPE                 [nt:base/@jcr:primaryType]
//                    +- string with value 'nt:childNodeDefinition'
//                +- REQUIRED_PRIMARY_TYPES       [nt:childNodeDefinition/@jcr:requiredPrimaryTypes]
//                    +- string values (limited to names)
//                +- DEFAULT_PRIMARY_TYPE         [nt:childNodeDefinition/@jcr:defaultPrimaryType]
//                    +- string value (limited to a name)
//                +- MANDATORY                    [nt:childNodeDefinition/@jcr:mandatory]
//                    +- string containing boolean value (or false if not present)
//                +- AUTO_CREATED                 [nt:childNodeDefinition/@jcr:autoCreated]
//                    +- string containing boolean value (or false if not present)
//                +- PROTECTED                    [nt:childNodeDefinition/@jcr:protected]
//                    +- string containing boolean value (or false if not present)
//                +- SAME_NAME_SIBLINGS           [nt:childNodeDefinition/@jcr:sameNameSiblings]
//                    +- string containing boolean value (or false if not present)
//                +- ON_PARENT_VERSION            [nt:childNodeDefinition/@jcr:onParentVersion]
//                    +- string value (limited to one of the predefined literal values)
//
// Comments
// --------
// The JSR-283 specification states that comments are allowed in CND files but are to be removed prior
// to parsing and processing.  This grammar accomplishes this by sending the MULTI_LINE_COMMENT and
// SINGLE_LINE_COMMENT tokens to the HIDDEN channel (along with all whitespace tokens).
//
// Case sensitivity
// ----------------
// ANTLR 3 has no way of generating a case-insensitive lexer/parser (since this is dependent upon locale).
// However, it's possible to do this as outlined at http://www.antlr.org/wiki/pages/viewpage.action?pageId=1782.
// Note that the approach that overrides the "LT" method does not change the content but merely changes the
// character returned by the method, which are the characters used to match the rules.
//
// And this must be done for this grammar, since the CND tokens are case-insensitive.  Note that this
// grammar defines all tokens as lower-case, so the ANTLRFileStream subclass must perform a 'toLowerCase()'
// rather than a 'toUpperCase()'.
//
// Post Processing
// ---------------
// A number of the values in the AST must be post-processed to perform the requisite validation.
//
// The string values may or may not be wrapped in quotes (single or double).  Any quotes will
// need to be removed in post-processing.
//
// Also, the QUERY_OPERATORS string value should contain a comma-separated list of operators 
// wrapped by single quotes.  As noted below, this is a side effect of how this grammar's lexer
// automatically produces a STRING token whenever single-quoted strings are found, so by the
// time the parser rules are run, the operators are encapsulated in a STRING token.
// (It's arguable whether this is a defect in the CND grammar, or a side effect of how this grammar
// uses ANTLR; other parser and lexer rules were considered, but most caused a massive increase in
// the size of the generated code and a massive decrease in performance.)  In short, these
// operators must be validated in a post-processing step.
//
// Generating JCR content
// ----------------------
//
// The resulting AST's structure was designed such that it directly corresponds to the "nt:nodeType", 
// "nt:propertyDefinition", and "nt:childNodeDefinition" node types that are specified in JSR-283
// (proposed draft), in the hopes that this approach could be used with a "standard" AST tree walker
// that could automatically generate the JCR content.
//
// First of all, the structure follows a repeating multi-level pattern, where the "NODE" appears 
// in the AST when a new node should be created, with JCR properties
// identified by AST nodes under "NODE" that have a single child (e.g., the property's value),
// or with JCR child nodes identified by AST nodes under "NODE" that also contain an AST "NODE"
// (with properties below it).
//
// The AST node names were designed to be easily translated into property names.  ANTLR uses
// all-caps for AST node names and '_' to separate words in those names.  To produce the JCR name,
// simply convert the first word to lowercase, and convert to lowercase all but the first character
// of each remaining word, and finally remove all '_' characters.
//
// This grammar (mostly) uses the "jcr" prefix (the namespaces should probably be "dna").
// This could be automated by defining a mapping between the AST node names and the property names
// (as well as defining a default namespace for any AST node name that is to be converted automatically).
//
cnd : (namespaceMapping|nodeTypeDefinition)* EOF
	-> ^(NAMESPACES namespaceMapping*)? ^(NODE_TYPES nodeTypeDefinition*)? ;

// Namespace mappings 
namespaceMapping : '<' prefix '=' uri '>' -> ^(NODE prefix uri);
prefix : STRING -> ^(PREFIX STRING);
uri : STRING -> ^(URI STRING);

// Node type definitions
nodeTypeDefinition :	nodeTypeName supertypes? nodeTypeOptions? ( propertyDefinition | childNodeDefinition )* 
	-> ^(NODE nodeTypeName ^(PRIMARY_TYPE STRING["nt:nodeType"]) supertypes? nodeTypeOptions? ^(PROPERTY_DEFINITION propertyDefinition*) ^(CHILD_NODE_DEFINITION childNodeDefinition*)) ;
nodeTypeName : '[' STRING ']' -> ^(NAME STRING) ;
supertypes : '>' stringList -> ^(SUPERTYPES stringList);
nodeTypeOptions :	nodeTypeOption+;
nodeTypeOption :	orderable | mixin | isAbstract | noQuery | primaryItem ;
orderable :	('o'|'ord'|'orderable') -> ^(HAS_ORDERABLE_CHILD_NODES STRING["true"]);
mixin :	('m' | 'mix' | 'mixin') -> ^(IS_MIXIN STRING["true"]);
isAbstract :	('a'|'abs'|'abstract') -> ^(IS_ABSTRACT STRING["true"]);
noQuery :	('nq'|'noquery') -> ^(IS_QUERYABLE STRING["false"]);
primaryItem : ('primaryitem'|'!')	STRING -> ^(PRIMARY_ITEM_NAME STRING);

// Property definitions ...
propertyDefinition : propertyName propertyType? defaultValues? ( propertyAttributes | valueConstraints )*
	-> ^(NODE propertyName ^(PRIMARY_TYPE STRING["nt:propertyDefinition"]) propertyType? defaultValues? propertyAttributes* valueConstraints*);
propertyName : '-' STRING -> ^(NAME STRING);
propertyType : '(' propertyTypeLiteral ')' -> ^(REQUIRED_TYPE propertyTypeLiteral); 
propertyTypeLiteral	: ('string'|'binary'|'long'|'double'|'boolean'|'date'|'name'|'path'|'reference'|'*');
defaultValues :	'=' stringList -> ^(DEFAULT_VALUES stringList);
propertyAttributes : ( (onParentVersioningLiteral)=>onParentVersioning | (autoCreated)=> autoCreated | (multiple)=>multiple | (mandatory)=>mandatory | (isProtected)=>isProtected | (queryOperators)=>queryOperators | (noFullText)=>noFullText | (noQueryOrder)=>noQueryOrder)+ ;
//propertyAttributes : ( (onParentVersioning)=> onParentVersioning | ('a')=>autoCreated | ('m')=>mandatory |('p')=>isProtected | ('mul'|'*')=>multiple | ('q')=>queryOperators | ('nof')=>noFullText | ('nqord'|'noq')=>noQueryOrder)+ ;
valueConstraints : '<' stringList -> ^(VALUE_CONSTRAINTS stringList);
autoCreated :	('a'|'aut'|'autocreated') -> ^(AUTO_CREATED STRING["true"]);
mandatory :	('m'|'man'|'mandatory') -> ^(MANDATORY STRING["true"]);
isProtected :	('p'|'pro'|'protected') -> ^(PROTECTED STRING["true"]);
onParentVersioning :	onParentVersioningLiteral -> ^(ON_PARENT_VERSION onParentVersioningLiteral);
onParentVersioningLiteral : ('copy'|'version'|'initialize'|'compute'|'ignore'|'abort');
multiple :	('*'|'mul'|'multiple') -> ^(MULTIPLE STRING["true"]);
noFullText :	('nof'|'nofulltext') -> ^(IS_FULL_TEXT_SEARCHABLE STRING["false"]);
noQueryOrder :	('nqord'|'noqueryorder') -> ^(IS_QUERY_ORDERERABLE STRING["false"]);
queryOperators :	('qop'|'queryops') STRING -> ^(QUERY_OPERATORS STRING);
// The grammar defines the query operators to be wrapped by single quotes, and therefore the lexer produces a single STRING token.
// Since we cannot break this token up, we simply store the operators as a STRING literal, and will have to process
// at a later step.

// Child node definitions ...
childNodeDefinition : nodeName requiredTypes? defaultType? nodeAttributes?
  -> ^(NODE nodeName ^(PRIMARY_TYPE STRING["nt:childNodeDefinition"]) requiredTypes? defaultType? nodeAttributes?);
nodeName : '+' STRING -> ^(NAME STRING);
requiredTypes :'(' stringList ')' -> ^(REQUIRED_PRIMARY_TYPES stringList); 
defaultType :	'=' STRING -> ^(DEFAULT_PRIMARY_TYPE STRING);
nodeAttributes : nodeAttribute+;
nodeAttribute : autoCreated | mandatory | isProtected | onParentVersioning | sns ;
sns : ('sns'|'*') -> ^(SAME_NAME_SIBLINGS STRING["true"]) ;

// General rules
stringList : STRING (',' STRING )* -> STRING*;

// ------------
// Lexer rules
// ------------

// Comments are currently sent to a separate channel
MULTI_LINE_COMMENT : ('/*' (options {greedy=false;} : . )* '*/') { $channel=HIDDEN;};
SINGLE_LINE_COMMENT : '//' ~('\n' | '\r')*  { $channel=HIDDEN;};

// Quoted strings allow for strings containing characters that otherwise would be CND delimiters.
// Note that the single- and double-quote characters must be escaped within the string.
// Also note that escape sequences are allowed as well.
STRING : QUOTED_STRING | UNQUOTED_STRING;

// Quoted strings may contain escaped characters.
fragment QUOTED_STRING
    : '"' ( EscapeSequence | ~('\\'|'"'))* '"'
    | '\'' ( EscapeSequence | ~('\\'|'\''))* '\'' 
    ;

fragment EscapeSequence 
  : '\\' ('b'|'t'|'n'|'f'|'r'|'\"'|'\''|'\\'
         |('0'..'3') (('0'..'7') ('0'..'7')?)?
         |'u'(('0'..'9')|('a'..'f')) (('0'..'9')|('a'..'f')) (('0'..'9')|('a'..'f'))
         )
;

// An unquoted string is a word delimited by whitespace and CND tokens.
fragment UNQUOTED_STRING
: (~(' '|'\r'|'\t'|'\u000C'|'\n'	// whitespace
    |'='|'<'|'>'|'['|']'|','|'-'|'('|')'	// tokens
  ))+;

WS : (' '|'\r'|'\t'|'\u000C'|'\n')+	{$channel=HIDDEN;} ;


