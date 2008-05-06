#!/bin/sh

java -Djava.ext.dirs=`pwd`/lib -cp .:dna-example-sequencers-0.1-SNAPSHOT.jar org.jboss.example.dna.sequencers.SequencingClient $1