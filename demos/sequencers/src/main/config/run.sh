#!/bin/sh

java -Dlogback.configurationFile=logback.xml -Djava.ext.dirs=`pwd`/lib -cp ${project.artifactId}-${project.version}.jar org.modeshape.demo.sequencer.SequencerDemo
