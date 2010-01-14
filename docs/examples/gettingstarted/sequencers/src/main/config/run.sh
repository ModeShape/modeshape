#!/bin/sh

java -Djava.ext.dirs=`pwd`/lib -cp .:modeshape-example-sequencers-0.1-SNAPSHOT.jar org.modeshape.example.sequencer.SequencingClient $1
