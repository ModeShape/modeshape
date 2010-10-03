@REM JBoss, Home of Professional Open Source.@REM Copyright (C) 2008 Red Hat, Inc.@REM Licensed to Red Hat, Inc. under one or more contributor @REM license agreements.  See the copyright.txt file in the@REM distribution for a full listing of individual contributors.@REM @REM This library is free software; you can redistribute it and/or@REM modify it under the terms of the GNU Lesser General Public@REM License as published by the Free Software Foundation; either@REM version 2.1 of the License, or (at your option) any later version.@REM @REM This library is distributed in the hope that it will be useful,@REM but WITHOUT ANY WARRANTY; without even the implied warranty of@REM MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU@REM Lesser General Public License for more details.@REM @REM You should have received a copy of the GNU Lesser General Public@REM License along with this library; if not, write to the Free Software@REM Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA@REM 02110-1301 USA.@REM This assumes it's run from its installation directory. It is also assumed there is a java@REM executable defined along the PATH

@REM   The ddl-gen.bat script is DDL generation utility used to create a
@REM 	ddl script based on the Hibernate dialect.
@REM   Running this will create two files in the output directory 
@REM	(or the current directory if no output directory was specified)@if not "%ECHO%" == ""  echo %ECHO%@if "%OS%" == "Windows_NT" set local

if not "%1"=="" goto cont @echo.
 @echo.
 @echo usage:  ddl-gen.bat -dialect (dialect name) -model (model_name) [-out (path to output directory)]
 @echo 		where dialect and model parameters should match the value of the dialect 
 @echo 			and model properties specified for the JPA connector.	
 @echo. 
 @echo Example: ddl-gen.bat -dialect HSQL -model Simple -out c:\temp
 @echo. 

  goto end

:cont

 @echo off
if "%OS%" == "Windows_NT" (  set "DIRNAME=%~dp0%") else (  set DIRNAME=.\)pushd %DIRNAME%if "x%MS_HOME%" == "x" (  set "MS_HOME=%CD%")popdset DIRNAME=if "x%JAVA_HOME%" == "x" (  set  JAVA=java  echo JAVA_HOME is not set. Unexpected results may occur.  echo Set JAVA_HOME to the directory of your local JDK to avoid this message.) else (  set "JAVA=%JAVA_HOME%\bin\java"  if exist "%JAVA_HOME%\lib\tools.jar" (    set "JAVAC_JAR=%JAVA_HOME%\lib\tools.jar"  ))rem JVM memory allocation pool parameters. Modify as appropriate.set JAVA_OPTS=%JAVA_OPTS% -Xms128m -Xmx256m -XX:MaxPermSize=256m"%JAVA%" %JAVA_OPTS% ^   -classpath ".\lib\*" ^   org.modeshape.util.SchemaGen %*

:end