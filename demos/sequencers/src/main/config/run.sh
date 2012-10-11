#!/bin/sh

java -Dlogback.configurationFile=logback.xml -Djava.ext.dirs=`pwd`/lib -cp .:modeshape-demo-sequencers-${project.version}.jar org.modeshape.demo.sequencer.SequencerDemo $1
