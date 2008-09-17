#!/bin/sh

java -Djava.ext.dirs=`pwd`/lib -cp .:dna-example-repositories-0.1-SNAPSHOT.jar org.jboss.example.dna.repository.RepositoryClient $1