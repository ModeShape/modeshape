#!/bin/sh
#
# JBoss, Home of Professional Open Source.
# Copyright (C) 2008 Red Hat, Inc.
# Licensed to Red Hat, Inc. under one or more contributor 
# license agreements.  See the copyright.txt file in the
# distribution for a full listing of individual contributors.
# 
# This library is free software; you can redistribute it and/or
# modify it under the terms of the GNU Lesser General Public
# License as published by the Free Software Foundation; either
# version 2.1 of the License, or (at your option) any later version.
# 
# This library is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
# Lesser General Public License for more details.
# 
# You should have received a copy of the GNU Lesser General Public
# License along with this library; if not, write to the Free Software
# Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
# 02110-1301 USA.


#  The ddl-gen script is DDL generation utility used to create a
#	ddl script based on the Hibernate dialect.
#   Running this will create two files in the output directory 
#	(or the current directory if no output directory was specified)

usage ()
{	
 echo
 echo
 echo "usage:  ddl-gen.sh -dialect <dialect name> -model <model_name> [-out <path to output directory>] [-delimiter <delim>]"
 echo "		where dialect and model parameters should match the value of the dialect and model properties"
 echo "				specified for the JPA connector.				"
 echo "    "
 echo "Example: ddl-gen.sh -dialect HSQL -model Simple -out /tmp -delimiter ;"
 echo "    "
 exit
}



if [ $# -lt 1 ]; then
	usage
fi

DIRNAME=`dirname $0`

# OS specific support (must be 'true' or 'false').
cygwin=false;
case "`uname`" in
    CYGWIN*)
        cygwin=true
        ;;
        
esac


# For Cygwin, ensure paths are in UNIX format before anything is touched
if $cygwin ; then
    [ -n "$MS_HOME" ] &&
        MS_HOME=`cygpath --unix "$MS_HOME"`
    [ -n "$JAVA_HOME" ] &&
        JAVA_HOME=`cygpath --unix "$JAVA_HOME"`
fi


# Setup ModeShape Home
if [ "x$MS_HOME" = "x" ]; then
    # get the full path (without any relative bits)
    MS_HOME=`cd $DIRNAME/..; pwd`
fi
export JBOSS_HOME

# Setup the JVM
if [ "x$JAVA" = "x" ]; then
    if [ "x$JAVA_HOME" != "x" ]; then
	JAVA="$JAVA_HOME/bin/java"
    else
	JAVA="java"
    fi
fi

# JPDA options. Uncomment and modify as appropriate to enable remote debugging.
# JAVA_OPTS="$JAVA_OPTS -Xdebug -Xrunjdwp:transport=dt_socket,address=8787,server=y,suspend=n"

# Setup JBoss sepecific properties
JAVA_OPTS="$JAVA_OPTS"

$JAVA $JAVA_OPTS -cp "./lib/*" -Xmx256m  org.modeshape.util.SchemaGen $*